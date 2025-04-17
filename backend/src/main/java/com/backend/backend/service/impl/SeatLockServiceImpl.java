package com.backend.backend.service.impl;

import com.backend.backend.entity.Screening;
import com.backend.backend.entity.SeatLock;
import com.backend.backend.entity.Room;
import com.backend.backend.dto.SeatIdentifier;
import com.backend.backend.exception.ScreeningNotFoundException;
import com.backend.backend.exception.SeatUnavailableException;
import com.backend.backend.mapper.SeatLockMapper;
import com.backend.backend.service.OrderSeatService;
import com.backend.backend.service.ScreeningService;
import com.backend.backend.service.SeatLockService;
import com.backend.backend.service.RoomService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@Service
public class SeatLockServiceImpl extends ServiceImpl<SeatLockMapper, SeatLock> implements SeatLockService {

    @Autowired
    private SeatLockMapper seatLockMapper;

    @Autowired // 注入 RoomService
    private RoomService roomService;

    @Autowired // Restore injection
    private OrderSeatService orderSeatService;

    @Autowired // Restore injection
    private ScreeningService screeningService;

    // Assuming lock duration is defined elsewhere, e.g., in application properties
    private static final int LOCK_DURATION_MINUTES = 15; 
    private static final int DEFAULT_LOCK_DURATION_SECONDS = LOCK_DURATION_MINUTES * 60;

    @Override
    public List<SeatLock> findActiveLocksByScreeningId(Long screeningId) {
        LambdaQueryWrapper<SeatLock> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SeatLock::getScreeningId, screeningId);
        wrapper.gt(SeatLock::getLockExpiryTime, LocalDateTime.now()); // 只查找未过期的
        return seatLockMapper.selectList(wrapper);
    }

    @Override
    @Transactional // 锁定操作需要事务保证原子性
    public List<SeatLock> lockSeats(Long screeningId, Long userId, List<SeatIdentifier> seatsToLock, int lockDurationSeconds) {
         // 1. 校验场次状态
        Screening screening = screeningService.getById(screeningId);
        if (screening == null) {
            throw new ScreeningNotFoundException("场次不存在: " + screeningId);
        }
        if (screening.getStatus() != Screening.ScreeningStatus.APPROVED || screening.getEndTime().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("场次状态不允许锁定座位");
        }

        // 1.5 获取影厅信息用于校验座位范围
        Room room = roomService.getById(screening.getRoomId());
        if (room == null || room.getRowsCount() == null || room.getColsCount() == null) {
            log.error("Room info or layout is invalid for screeningId: {}, roomId: {}", screeningId, screening.getRoomId());
            throw new RuntimeException("无法获取影厅布局信息"); // 或者更具体的异常
        }
        int maxRows = room.getRowsCount();
        int maxCols = room.getColsCount();

        // 2. 检查座位是否有效、重复、超出范围
        Set<SeatIdentifier> uniqueSeats = new HashSet<>(seatsToLock);
        if (uniqueSeats.size() != seatsToLock.size()) {
            throw new IllegalArgumentException("请求中包含重复的座位");
        }
        for (SeatIdentifier seat : uniqueSeats) {
            if (seat.getRowIndex() <= 0 || seat.getRowIndex() > maxRows ||
                seat.getColIndex() <= 0 || seat.getColIndex() > maxCols) {
                throw new IllegalArgumentException("包含无效的座位号 (超出影厅范围): [" + seat.getRowIndex() + "," + seat.getColIndex() + "]");
            }
        }

        // 3. 检查座位是否已被他人锁定 (使用 findAnyActiveLocksForSeats)
        List<SeatLock> existingLocks = seatLockMapper.findAnyActiveLocksForSeats(screeningId, seatsToLock, LocalDateTime.now());
        if (!existingLocks.isEmpty()) {
             // Filter locks not held by the current user
            List<SeatLock> otherUserLocks = existingLocks.stream()
                .filter(lock -> !lock.getUserId().equals(userId))
                .collect(Collectors.toList());
            if (!otherUserLocks.isEmpty()) {
                 String lockedSeatsStr = otherUserLocks.stream()
                    .map(l -> "[" + l.getRowIndex() + "," + l.getColIndex() + "]")
                    .collect(Collectors.joining(", "));
                throw new SeatUnavailableException("部分或全部座位已被他人锁定: " + lockedSeatsStr);
            }
             // If locks exist but are held by the current user, maybe refresh their expiry? Or ignore.
             // For now, we'll proceed, assuming the intention is to potentially extend the lock or handle it.
             log.info("User {} attempting to re-lock already held seats for screening {}. Proceeding.", userId, screeningId);
        }

        // 4. 检查座位是否已售 (使用 findSoldSeatsForScreening)
        List<SeatIdentifier> soldSeats = seatLockMapper.findSoldSeatsForScreening(screeningId, seatsToLock);
         if (!soldSeats.isEmpty()) {
             String soldSeatsStr = soldSeats.stream()
                .map(s -> "[" + s.getRowIndex() + "," + s.getColIndex() + "]")
                .collect(Collectors.joining(", "));
            throw new SeatUnavailableException("部分或全部座位已被售出: " + soldSeatsStr);
        }
        
        // 5. 准备批量插入的数据
        LocalDateTime expiryTime = LocalDateTime.now().plusSeconds(lockDurationSeconds > 0 ? lockDurationSeconds : DEFAULT_LOCK_DURATION_SECONDS);
        List<SeatLock> lockEntities = new ArrayList<>();
        for (SeatIdentifier seat : uniqueSeats) {
            SeatLock lock = new SeatLock();
            lock.setScreeningId(screeningId);
            lock.setRowIndex(seat.getRowIndex());
            lock.setColIndex(seat.getColIndex());
            lock.setUserId(userId);
            lock.setLockExpiryTime(expiryTime);
            // Only add if not already locked by the current user (or implement upsert logic)
            boolean alreadyLockedByUser = existingLocks.stream().anyMatch(l -> l.getUserId().equals(userId) && l.getRowIndex().equals(seat.getRowIndex()) && l.getColIndex().equals(seat.getColIndex()));
            if (!alreadyLockedByUser) {
                 lockEntities.add(lock);
            }
        }

        // 6. 尝试批量插入 (乐观锁通过数据库唯一约束实现)
        if (!lockEntities.isEmpty()) {
            try {
                boolean success = this.saveBatch(lockEntities);
                if (!success) {
                    log.warn("saveBatch returned false for screening {} user {}. Assuming lock conflict.", screeningId, userId);
                    throw new SeatUnavailableException("部分或全部座位已被他人锁定 (saveBatch)");
                }
                log.info("Successfully locked {} new seats for screening {} user {}", lockEntities.size(), screeningId, userId);
            } catch (DataIntegrityViolationException e) {
                Throwable cause = e.getCause();
                String message = "部分或全部座位已被他人锁定 (constraint)";
                if (cause != null && cause.getMessage() != null) {
                    log.warn("Seat lock conflict detail for screening {}: {}", screeningId, cause.getMessage());
                }
                throw new SeatUnavailableException(message, e);
            } catch (Exception e) {
                log.error("Error locking seats for screening {} user {}: {}", screeningId, userId, e.getMessage(), e);
                throw new RuntimeException("锁定座位时发生未知错误", e);
            }
        } else {
             // If all seats were already locked by the user, return the existing locks found earlier
             // Need to fetch them again specifically for the user to be precise
             // This part needs refinement based on desired behavior for re-locking
             log.info("No new seats to lock for user {} screening {}. Returning existing locks (if any).", userId, screeningId);
             // return existingLocks.stream().filter(l -> l.getUserId().equals(userId)).collect(Collectors.toList()); // Option: return existing user locks
             return new ArrayList<>(existingLocks.stream().filter(l -> l.getUserId().equals(userId)).collect(Collectors.toList())); // Ensure mutable list
        }
        
        // Fix: Return the list of newly created lock entities
        return lockEntities; 
    }
    
    @Override
    public List<SeatLock> findActiveUserLocksForSeats(Long userId, Long screeningId, List<SeatIdentifier> seats) {
        if (CollectionUtils.isEmpty(seats)) {
            return List.of(); // Return empty list if no seats are provided
        }
        LocalDateTime now = LocalDateTime.now();
        // Use the specific mapper method
        return seatLockMapper.findActiveUserLocksForSeats(userId, screeningId, seats, now);
    }

    @Override
    public boolean unlockSeats(Long screeningId, Long userId, List<SeatIdentifier> seats) {
        if (CollectionUtils.isEmpty(seats)) {
            return true; // Nothing to unlock
        }
        try {
            // Use the specific mapper method
            int deletedRows = seatLockMapper.deleteUserLocksForSeats(userId, screeningId, seats);
            log.info("Attempted to unlock {} seats for user {} screening {}. Rows affected: {}", seats.size(), userId, screeningId, deletedRows);
            return true; 
        } catch (Exception e) {
            log.error("Error unlocking seats for user {} screening {}: {}", userId, screeningId, e.getMessage(), e);
            return false; // Indicate failure
        }
    }

    @Override
    public int deleteExpiredLocks(LocalDateTime expiryThreshold) {
        int deletedCount = 0;
        try {
            // Use the specific mapper method
            deletedCount = seatLockMapper.deleteExpiredLocks(expiryThreshold);
            if (deletedCount > 0) {
                log.info("Successfully deleted {} expired seat locks older than {}.", deletedCount, expiryThreshold);
            }
        } catch (Exception e) {
            log.error("Error deleting expired seat locks older than {}: {}", expiryThreshold, e.getMessage(), e);
        }
        return deletedCount; // Return the count
    }
} 