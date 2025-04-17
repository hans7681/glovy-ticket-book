package com.backend.backend.service.impl;

import com.backend.backend.entity.OrderSeat;
import com.backend.backend.entity.Room;
import com.backend.backend.entity.Screening;
import com.backend.backend.entity.SeatLock;
import com.backend.backend.service.*;
import com.backend.backend.vo.SeatStatusVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SeatServiceImpl implements SeatService {

    @Autowired
    private ScreeningService screeningService;

    @Autowired
    private RoomService roomService;

    @Autowired
    private OrderSeatService orderSeatService; // 需要创建 OrderSeatService

    @Autowired
    private SeatLockService seatLockService;

    @Override
    public List<List<SeatStatusVO>> getSeatStatus(Long screeningId) {
        // 1. 获取场次信息，主要是为了拿到 roomId
        Screening screening = screeningService.getById(screeningId);
        if (screening == null) {
            throw new RuntimeException("场次不存在: " + screeningId);
        }
        // 可以在这里增加对场次状态的校验，例如只有 APPROVED 且未结束的场次才允许查看座位
        if (screening.getStatus() != Screening.ScreeningStatus.APPROVED || screening.getEndTime().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("场次状态不允许查看座位图");
        }

        // 2. 获取影厅布局信息
        Room room = roomService.getById(screening.getRoomId());
        if (room == null || room.getRowsCount() == null || room.getColsCount() == null) {
            throw new RuntimeException("影厅信息或座位布局无效: Room ID " + screening.getRoomId());
        }
        int rows = room.getRowsCount();
        int cols = room.getColsCount();
        // TODO: 解析 room.getSeatTemplate() 获取不可用座位信息

        // 3. 获取已售座位 (需要 OrderSeatService)
        List<OrderSeat> soldSeats = orderSeatService.findSeatsByScreeningId(screeningId);
        Set<String> soldSeatKeys = soldSeats.stream()
                .map(seat -> seat.getRowIndex() + "-" + seat.getColIndex())
                .collect(Collectors.toSet());

        // 4. 获取当前有效的锁定座位
        List<SeatLock> lockedSeats = seatLockService.findActiveLocksByScreeningId(screeningId);
        Set<String> lockedSeatKeys = lockedSeats.stream()
                .map(lock -> lock.getRowIndex() + "-" + lock.getColIndex())
                .collect(Collectors.toSet());

        // 5. 构建座位图
        List<List<SeatStatusVO>> seatMap = new ArrayList<>(rows);
        for (int i = 1; i <= rows; i++) {
            List<SeatStatusVO> rowSeats = new ArrayList<>(cols);
            for (int j = 1; j <= cols; j++) {
                String seatKey = i + "-" + j;
                SeatStatusVO seatVO = new SeatStatusVO();
                seatVO.setRowIndex(i);
                seatVO.setColIndex(j);

                // 判断状态: 已售 > 锁定 > 不可用(TODO) > 可用
                if (soldSeatKeys.contains(seatKey)) {
                    seatVO.setStatus(SeatStatusVO.SeatState.SOLD);
                } else if (lockedSeatKeys.contains(seatKey)) {
                    seatVO.setStatus(SeatStatusVO.SeatState.LOCKED);
                } /* else if (isUnavailable(i, j, room.getSeatTemplate())) { // TODO
                    seatVO.setStatus(SeatStatusVO.SeatState.UNAVAILABLE);
                } */ else {
                    seatVO.setStatus(SeatStatusVO.SeatState.AVAILABLE);
                }
                rowSeats.add(seatVO);
            }
            seatMap.add(rowSeats);
        }

        return seatMap;
    }

    // TODO: 实现解析 seatTemplate 判断座位是否不可用的逻辑
    // private boolean isUnavailable(int row, int col, String seatTemplateJson) { ... }
} 