package com.backend.backend.service.impl;

import com.backend.backend.dto.CreateOrderRequest;
import com.backend.backend.dto.SeatIdentifier;
import com.backend.backend.entity.*;
import com.backend.backend.exception.InvalidOperationException;
import com.backend.backend.exception.InvalidScreeningStatusException;
import com.backend.backend.exception.ResourceNotFoundException;
import com.backend.backend.exception.ScreeningNotFoundException;
import com.backend.backend.exception.SeatLockExpiredException;
import com.backend.backend.mapper.OrderMapper;
import com.backend.backend.mapper.OrderSeatMapper;
import com.backend.backend.service.*;
import com.backend.backend.util.OrderNumberGenerator; // Assuming a utility class for order number generation
import com.backend.backend.vo.OrderVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderSeatService orderSeatService; // Inject OrderSeatService

    @Autowired
    private ScreeningService screeningService;

    @Autowired
    private MovieService movieService;

    @Autowired
    private CinemaService cinemaService;

    @Autowired
    private RoomService roomService;

    @Autowired
    private SeatLockService seatLockService; // Inject SeatLockService

    @Autowired
    private OrderNumberGenerator orderNumberGenerator; // Inject order number generator

    @Autowired
    private OrderSeatMapper orderSeatMapper; // 注入 OrderSeatMapper

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public OrderVO createOrder(CreateOrderRequest request, Long userId) {
        log.info("Attempting to create order for user {} and screening {}", userId, request.getScreeningId());

        // Step 1: Get the list from the request
        var requestSeatInfos = request.getSelectedSeats();
        if (CollectionUtils.isEmpty(requestSeatInfos)) {
            log.warn("Order creation failed for user {}: No seats selected.", userId);
            throw new IllegalArgumentException("必须选择至少一个座位");
        }

        // Remove unnecessary and incorrect type conversions
        // List<SeatInfo> dtoSeatInfos = ...
        // List<SeatIdentifier> selectedSeats = ...

        Long screeningId = request.getScreeningId();

        // 1. Validate Screening
        Screening screening = validateScreening(screeningId);

        // 2. Validate Seat Locks (Crucial Step)
        // Convert SeatInfo list to SeatIdentifier list for validation method
        List<SeatIdentifier> seatsToValidate = requestSeatInfos.stream()
            .map(info -> new SeatIdentifier(info.getRowIndex(), info.getColIndex()))
            .collect(Collectors.toList());
        // Fetch active locks for these seats *for this user* within the transaction
        List<SeatLock> userLocks = findAndValidateUserLocks(userId, screeningId, seatsToValidate);

        // 3. Calculate total amount (use size from original request list)
        BigDecimal totalAmount = screening.getPrice().multiply(BigDecimal.valueOf(requestSeatInfos.size()));

        // 4. Generate Order Number
        String orderNo = orderNumberGenerator.generateOrderNumber();

        // 5. Create and Save Order
        Order order = createAndSaveOrder(userId, screening, orderNo, totalAmount, requestSeatInfos.size());

        // 6. Create and Save Order Seats (Pass the original SeatInfo list)
        List<OrderSeat> orderSeats = createAndSaveOrderSeats(order.getId(), screeningId, requestSeatInfos);

        // 7. Delete Seat Locks
        // Convert SeatInfo list to SeatIdentifier list for deletion method
        List<SeatIdentifier> seatsToUnlock = seatsToValidate; // Reuse the list created earlier
        deleteSeatLocks(userId, screeningId, seatsToUnlock);

        log.info("Order {} created successfully for user {}", order.getOrderNo(), userId);

        // 8. Assemble and return OrderVO
        return assembleOrderVO(order, screening, orderSeats);
    }

    private Screening validateScreening(Long screeningId) {
        Screening screening = screeningService.getById(screeningId);
        if (screening == null) {
            log.warn("Order creation failed: Screening not found for ID {}", screeningId);
            throw new ScreeningNotFoundException("场次不存在: " + screeningId);
        }
        if (screening.getStatus() != Screening.ScreeningStatus.APPROVED) {
            log.warn("Order creation failed: Screening {} status is {}, not APPROVED", screeningId, screening.getStatus());
            throw new InvalidScreeningStatusException("该场次当前状态不允许购票: " + screening.getStatus());
        }
        if (screening.getStartTime().isBefore(LocalDateTime.now())) {
            log.warn("Order creation failed: Screening {} has already started or ended ({})", screeningId, screening.getStartTime());
            throw new InvalidScreeningStatusException("该场次已开始或已结束");
        }
        log.debug("Screening {} validated successfully.", screeningId);
        return screening;
    }

    private List<SeatLock> findAndValidateUserLocks(Long userId, Long screeningId, List<SeatIdentifier> seats) {
        // We need a way to query locks for specific seats for a specific user
        // Option 1: Fetch all active locks for the screening and filter
        // Option 2: Add a method to SeatLockService/Mapper to query by user, screening, and list of seats
        
        // Using Option 2 for better performance and clarity (assuming it exists or we add it)
        List<SeatLock> foundLocks = seatLockService.findActiveUserLocksForSeats(userId, screeningId, seats);

        // Validate if all requested seats were found and locked by the current user
        if (foundLocks.size() != seats.size()) {
            Set<SeatIdentifier> requestedSet = seats.stream().collect(Collectors.toSet());
            Set<SeatIdentifier> foundSet = foundLocks.stream()
                .map(lock -> new SeatIdentifier(lock.getRowIndex(), lock.getColIndex()))
                .collect(Collectors.toSet());
            requestedSet.removeAll(foundSet);
            String missingSeatsStr = requestedSet.stream()
                .map(s -> "[" + s.getRowIndex() + "," + s.getColIndex() + "]")
                .collect(Collectors.joining(", "));
                
            log.warn("Order creation failed for user {}: Seat locks not found or expired for seats: {}", userId, missingSeatsStr);
            throw new SeatLockExpiredException("部分或全部座位的锁定已失效或不存在: " + missingSeatsStr);
        }

        // Additional check: ensure expiry time is still valid (although findActiveUserLocksForSeats should handle this)
        LocalDateTime now = LocalDateTime.now();
        for (SeatLock lock : foundLocks) {
            if (lock.getLockExpiryTime().isBefore(now)) {
                 log.warn("Order creation failed for user {}: Seat lock expired for seat [{},{}] at {}", userId, lock.getRowIndex(), lock.getColIndex(), lock.getLockExpiryTime());
                 throw new SeatLockExpiredException("座位 ["+lock.getRowIndex()+","+lock.getColIndex()+"] 的锁定已过期");
            }
        }

        log.debug("All {} seat locks validated for user {}", seats.size(), userId);
        return foundLocks; // Return the validated locks (though we might not need them later)
    }

    private Order createAndSaveOrder(Long userId, Screening screening, String orderNo, BigDecimal totalAmount, int seatCount) {
        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setScreeningId(screening.getId());
        order.setCinemaId(screening.getCinemaId()); // Redundant field
        order.setMovieId(screening.getMovieId());   // Redundant field
        order.setTotalAmount(totalAmount);
        order.setSeatCount(seatCount);
        order.setStatus(Order.OrderStatus.PENDING_PAYMENT); // Initial status
        // createTime and updateTime should be handled by MyBatis Plus or DB

        boolean saved = this.save(order);
        if (!saved || order.getId() == null) {
            log.error("Failed to save order for user {} and screening {}", userId, screening.getId());
            throw new RuntimeException("创建订单记录失败");
        }
        log.debug("Order record {} saved with ID {}", orderNo, order.getId());
        return order;
    }

    private List<OrderSeat> createAndSaveOrderSeats(Long orderId, Long screeningId, List<CreateOrderRequest.SeatInfo> selectedSeats) {
        List<OrderSeat> orderSeats = new ArrayList<>();
        for (CreateOrderRequest.SeatInfo seat : selectedSeats) { // Loop through SeatInfo
            OrderSeat orderSeat = new OrderSeat();
            orderSeat.setOrderId(orderId);
            orderSeat.setScreeningId(screeningId);
            orderSeat.setRowIndex(seat.getRowIndex()); // Use SeatInfo getter
            orderSeat.setColIndex(seat.getColIndex()); // Use SeatInfo getter
            // Add generation and setting of seat label using the helper method
            orderSeat.setSeatLabel(generateSeatLabel(seat.getRowIndex(), seat.getColIndex())); // Use SeatInfo getter
            orderSeats.add(orderSeat);
        }

        // Batch insert the created order seats
        if (!CollectionUtils.isEmpty(orderSeats)) {
             boolean seatsSaved = orderSeatService.saveBatch(orderSeats);

             if (!seatsSaved) {
                  log.error("Failed to save all order seats for order ID {}. Rolling back.", orderId);
                  throw new RuntimeException("创建订单座位记录失败 (batch save failed)");
             }
             log.debug("{} order seat records saved for order ID {}", orderSeats.size(), orderId);
        } else {
             log.warn("Attempted to save order seats, but the list of seats was empty for order ID {}", orderId);
        }

        return orderSeats;
    }

    private void deleteSeatLocks(Long userId, Long screeningId, List<SeatIdentifier> seatsToUnlock) {
        if (!CollectionUtils.isEmpty(seatsToUnlock)) {
            try {
                boolean unlocked = seatLockService.unlockSeats(screeningId, userId, seatsToUnlock);
                if (unlocked) {
                    log.info("Successfully unlocked {} seats for user {} after order creation.", seatsToUnlock.size(), userId);
                } else {
                    // This might indicate an issue, but the order is already created.
                    // Maybe the locks expired between validation and deletion?
                    log.warn("Seat unlock operation returned false for user {} after order creation. Locks might have already expired or been deleted.", userId);
                }
            } catch (Exception e) {
                // Log error but don't fail the transaction as order is already created
                log.error("Error occurred while trying to delete seat locks for user {} after order {} creation: {}", 
                          userId, /* Need orderNo here if available */ "N/A", e.getMessage(), e);
            }
        } else {
             log.warn("Attempted to delete seat locks, but the list of seats to unlock was empty for user {}", userId);
        }
    }
    
    private OrderVO assembleOrderVO(Order order, Screening screening, List<OrderSeat> orderSeats) {
        OrderVO vo = new OrderVO();
        BeanUtils.copyProperties(order, vo); // Copy basic order fields
        vo.setOrderId(order.getId()); // Ensure ID is copied if not already
        
        // Populate related entity names/details
        // These might require fetching if not already available
        Movie movie = movieService.getById(screening.getMovieId());
        Cinema cinema = cinemaService.getById(screening.getCinemaId());
        Room room = roomService.getById(screening.getRoomId());

        if (movie != null) {
            vo.setMovieTitle(movie.getTitle());
        }
        if (cinema != null) {
            vo.setCinemaName(cinema.getName());
        }
        if (room != null) {
            vo.setRoomName(room.getName());
        }
        vo.setScreeningStartTime(screening.getStartTime());
        
        // Create seat descriptions
        List<String> seatDescriptions = orderSeats.stream()
            .map(seat -> seat.getRowIndex() + "排" + seat.getColIndex() + "座")
            .collect(Collectors.toList());
        vo.setSeatsDescription(seatDescriptions);
        
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelOrder(Long orderId, Long userId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        // 权限校验：是否是自己的订单
        if (!Objects.equals(order.getUserId(), userId)) {
            throw new RuntimeException("无权操作该订单");
        }

        // 状态校验：只有待支付或已支付且未开场的可以取消
        boolean canCancel = false;
        if (order.getStatus() == Order.OrderStatus.PENDING_PAYMENT) {
            canCancel = true;
        }
        if (order.getStatus() == Order.OrderStatus.PAID) {
            Screening screening = screeningService.getById(order.getScreeningId());
            if (screening != null && screening.getStartTime().isAfter(LocalDateTime.now())) {
                canCancel = true;
                // TODO: 如果已支付，需要触发退款流程
            }
        }

        if (!canCancel) {
            throw new RuntimeException("当前订单状态不允许取消");
        }

        // 1. 更新订单状态
        order.setStatus(Order.OrderStatus.CANCELLED);
        order.setCancelTime(LocalDateTime.now());
        int updatedOrderRows = orderMapper.updateById(order);
        if (updatedOrderRows <= 0) {
            throw new RuntimeException("更新订单状态失败");
        }

        // 2. 释放座位 (删除 order_seat 记录)
        int deletedSeats = orderSeatService.deleteByOrderId(orderId);
        // 这里可以根据 deletedSeats 数量判断是否符合预期

        return true;
    }

    @Override
    public Order getOrderDetails(Object identifier, Long userId) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();

        if (identifier instanceof String) {
            String idStr = (String) identifier;
            try {
                // Try parsing as Long ID first
                Long idLong = Long.parseLong(idStr);
                wrapper.eq(Order::getId, idLong); // Query by numeric ID
            } catch (NumberFormatException e) {
                // If not a number, assume it's an Order Number
                wrapper.eq(Order::getOrderNo, idStr); // Query by Order Number string
            }
        } else if (identifier instanceof Long) { // Handle direct Long input (e.g., from internal calls)
             wrapper.eq(Order::getId, identifier);
        } else {
            log.warn("Invalid identifier type received in getOrderDetails: {}", identifier != null ? identifier.getClass().getName() : "null");
            return null; // Invalid identifier type
        }

        // 如果提供了 userId，则添加用户匹配条件
        if (userId != null) {
            wrapper.eq(Order::getUserId, userId);
        }

        // 查询订单 (MyBatis Plus 的 selectOne 会自动加 LIMIT 1)
        Order order = orderMapper.selectOne(wrapper);

        // 填充关联信息 (座位、场次、电影)
        if (order != null) {
            order.setSeats(orderSeatService.findByOrderId(order.getId()));
            order.setScreening(screeningService.getById(order.getScreeningId()));
            if (order.getScreening() != null) {
                order.setMovie(movieService.getById(order.getScreening().getMovieId()));
            }
        }

        return order;
    }

    @Override
    public Page<Order> listUserOrders(Page<Order> page, Long userId, Order.OrderStatus status) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getUserId, userId);
        if (status != null) {
            wrapper.eq(Order::getStatus, status);
        }
        wrapper.orderByDesc(Order::getCreateTime);
        // 查询订单基本信息
        Page<Order> resultPage = orderMapper.selectPage(page, wrapper);
        // 填充关联信息
        if (resultPage != null && !CollectionUtils.isEmpty(resultPage.getRecords())) {
             resultPage.getRecords().forEach(order -> {
                 order.setScreening(screeningService.getById(order.getScreeningId()));
                 if (order.getScreening() != null) {
                     order.setMovie(movieService.getById(order.getScreening().getMovieId()));
                 }
                 // 可选：填充座位信息，但列表页通常不需要这么详细
                 // order.setSeats(orderSeatService.findByOrderId(order.getId()));
             });
        }
        return resultPage;
    }

    @Override
    public Page<Order> listCinemaOrders(Page<Order> page, Long cinemaId, Long adminUserId, Order.OrderStatus status) {
        // 权限校验：检查 adminUserId 是否管理 cinemaId
        Cinema cinema = cinemaService.getCinemaByAdminUserId(adminUserId);
        if (cinema == null || !cinema.getId().equals(cinemaId)) {
             throw new RuntimeException("无权查看该影院的订单");
        }

        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getCinemaId, cinemaId);
        if (status != null) {
            wrapper.eq(Order::getStatus, status);
        }
        wrapper.orderByDesc(Order::getCreateTime);
        // 查询并填充关联信息（同 listUserOrders）
         Page<Order> resultPage = orderMapper.selectPage(page, wrapper);
        if (resultPage != null && !CollectionUtils.isEmpty(resultPage.getRecords())) {
             resultPage.getRecords().forEach(order -> {
                 order.setScreening(screeningService.getById(order.getScreeningId()));
                 if (order.getScreening() != null) {
                     order.setMovie(movieService.getById(order.getScreening().getMovieId()));
                 }
             });
        }
        return resultPage;
    }

    @Override
    public Page<Order> listAllOrders(Page<Order> page, Long userId, Long cinemaId, Order.OrderStatus status) {
         LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
         if (userId != null) {
             wrapper.eq(Order::getUserId, userId);
         }
         if (cinemaId != null) {
             wrapper.eq(Order::getCinemaId, cinemaId);
         }
         if (status != null) {
            wrapper.eq(Order::getStatus, status);
         }
        wrapper.orderByDesc(Order::getCreateTime);
         // 查询并填充关联信息（同上）
         Page<Order> resultPage = orderMapper.selectPage(page, wrapper);
        if (resultPage != null && !CollectionUtils.isEmpty(resultPage.getRecords())) {
             resultPage.getRecords().forEach(order -> {
                 order.setScreening(screeningService.getById(order.getScreeningId()));
                 if (order.getScreening() != null) {
                     order.setMovie(movieService.getById(order.getScreening().getMovieId()));
                 }
             });
        }
        return resultPage;
    }

    // 生成座位标签，例如 行1列5 -> A5
    private String generateSeatLabel(int row, int col) {
        // 简单的用字母表示行，但超过26行会出问题，可以考虑其他方式
        char rowChar = (char) ('A' + row - 1);
        return String.valueOf(rowChar) + col;
    }

    @Override
    @Transactional(rollbackFor = Exception.class) // Ensure atomicity
    public Order markOrderAsPaid(Object identifier, Long userId) { // Explicitly matching interface types
        log.info("Attempting to mark order identified by '{}' as PAID for user {}", identifier, userId);

        // 1. Fetch the order and verify ownership
        // We rely on getOrderDetails to handle identifier parsing AND ownership check for the given userId
        Order order = getOrderDetails(identifier, userId);
        if (order == null) {
            // If getOrderDetails returns null, it means not found OR not owned by this user
            log.warn("Order not found or not owned by user {}. Identifier: {}", userId, identifier);
            throw new ResourceNotFoundException("订单不存在或无权访问");
        }

        // 2. Check current status
        if (order.getStatus() != Order.OrderStatus.PENDING_PAYMENT) {
            log.warn("Cannot mark order {} as PAID. Current status: {}", order.getOrderNo(), order.getStatus());
            throw new InvalidOperationException("订单状态不是待支付，无法标记为已支付 (当前状态: " + order.getStatus() + ")");
        }

        // 3. Update status and payment time
        order.setStatus(Order.OrderStatus.PAID);
        order.setPaymentTime(LocalDateTime.now());
        // update_time should be handled automatically by MyBatis Plus / DB

        // 4. Persist changes
        boolean success = this.updateById(order);
        if (!success) {
            log.error("Failed to update order {} to PAID status in database.", order.getOrderNo());
            // This might indicate a concurrent update issue or DB error
            throw new RuntimeException("更新订单状态失败");
        }

        log.info("Order {} successfully marked as PAID for user {}", order.getOrderNo(), userId);

        // 5. Return the updated order in memory
        return order;
    }

    @Override
    @Transactional(rollbackFor = Exception.class) // 确保事务性
    public int cancelTimedOutPendingOrdersAndReleaseSeats(LocalDateTime cutoffTime) {
        // 步骤 1: 查找超时的待支付订单 ID
        List<Long> timedOutOrderIds = orderMapper.findTimedOutPendingOrderIds(cutoffTime);

        if (CollectionUtils.isEmpty(timedOutOrderIds)) {
            // log.debug("没有找到创建时间早于 {} 的待支付订单。", cutoffTime);
            return 0; // 没有需要处理的订单
        }

        log.info("找到 {} 个超时未支付订单 (创建于 {} 之前)，准备取消并释放座位。 订单ID: {}", 
                 timedOutOrderIds.size(), cutoffTime, timedOutOrderIds);

        LocalDateTime cancelTime = LocalDateTime.now();

        try {
            // 步骤 2: 删除这些订单关联的座位记录 (释放座位)
            // 注意：如果 order_seat 表与 t_order 表有外键约束且设置了 ON DELETE CASCADE，
            // 则不需要手动删除 order_seat，但显式删除更清晰。
            int deletedSeatsCount = orderSeatMapper.deleteSeatsByOrderIds(timedOutOrderIds);
            log.info("为 {} 个超时订单删除了 {} 条座位记录。", timedOutOrderIds.size(), deletedSeatsCount);

            // 步骤 3: 批量更新订单状态为 CANCELLED
            int cancelledOrdersCount = orderMapper.batchCancelOrders(timedOutOrderIds, cancelTime);
            log.info("成功将 {} 个订单的状态更新为 CANCELLED。", cancelledOrdersCount);

            // 理论上 cancelledOrdersCount 应该等于 timedOutOrderIds.size()
            // 如果不一致，可能表示部分订单在查询后、更新前状态发生了变化
            if (cancelledOrdersCount != timedOutOrderIds.size()) {
                 log.warn("批量取消订单时，预期取消 {} 个，实际取消 {} 个。可能存在并发状态变更。",
                          timedOutOrderIds.size(), cancelledOrdersCount);
            }

            return cancelledOrdersCount;

        } catch (Exception e) {
            log.error("在取消超时订单 (IDs: {}) 并释放座位时发生数据库错误: {}", timedOutOrderIds, e.getMessage(), e);
            // 由于 @Transactional 注解，这里抛出异常将导致事务回滚
            throw new RuntimeException("取消超时订单并释放座位时发生错误", e);
        }
    }
} 
