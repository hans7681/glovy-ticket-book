package com.backend.backend.service;

import com.backend.backend.dto.CreateOrderRequest; // 需要创建
import com.backend.backend.entity.Order;
import com.backend.backend.entity.OrderSeat;
import com.backend.backend.vo.OrderVO; // Import the new VO
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderService extends IService<Order> {

    /**
     * 创建新订单
     * @param request 订单创建请求，包含场次ID和选中的座位列表
     * @param userId 当前用户ID
     * @return 创建成功的订单详情VO
     * @throws com.backend.backend.exception.ScreeningNotFoundException 如果场次不存在
     * @throws com.backend.backend.exception.InvalidScreeningStatusException 如果场次状态无效
     * @throws com.backend.backend.exception.SeatLockExpiredException 如果座位锁定失效或不存在
     * @throws com.backend.backend.exception.SeatsUnavailableException 如果座位已被购买 (虽然锁定应该避免，但作为额外检查)
     */
    OrderVO createOrder(CreateOrderRequest request, Long userId);

    /**
     * 取消订单
     * @param orderId 要取消的订单ID
     * @param userId 当前用户ID (用于权限验证)
     * @return 是否取消成功
     */
    boolean cancelOrder(Long orderId, Long userId);

    /**
     * 获取订单详情
     * @param identifier 订单ID (Long) 或订单号 (String)
     * @param userId 当前用户ID (如果为null，则不限制用户，通常用于管理员场景)
     * @return 订单详情实体，包含关联信息
     */
    Order getOrderDetails(Object identifier, Long userId);

    /**
     * 分页获取当前用户的订单列表
     * @param page 分页参数
     * @param userId 用户ID
     * @param status 订单状态 (可选筛选条件)
     * @return 订单分页结果
     */
    Page<Order> listUserOrders(Page<Order> page, Long userId, Order.OrderStatus status);

    /**
     * 分页获取指定影院的订单列表 (影院管理员使用)
     * @param page 分页参数
     * @param cinemaId 影院ID
     * @param adminUserId 影院管理员用户ID (用于权限验证)
     * @param status 订单状态 (可选筛选条件)
     * @return 订单分页结果
     */
    Page<Order> listCinemaOrders(Page<Order> page, Long cinemaId, Long adminUserId, Order.OrderStatus status);

    /**
     * 分页获取所有订单列表 (系统管理员使用)
     * @param page 分页参数
     * @param userId 用户ID (可选筛选条件)
     * @param cinemaId 影院ID (可选筛选条件)
     * @param status 订单状态 (可选筛选条件)
     * @return 订单分页结果
     */
    Page<Order> listAllOrders(Page<Order> page, Long userId, Long cinemaId, Order.OrderStatus status);

    /**
     * 标记订单为已支付。
     * 只能标记状态为 PENDING_PAYMENT 的订单。
     *
     * @param identifier 订单ID (Long) 或订单号 (String)
     * @param userId  当前操作用户 ID，用于权限校验（确保用户只能修改自己的订单）
     * @return 更新后的订单实体
     * @throws com.backend.backend.exception.ResourceNotFoundException 如果订单不存在或不属于该用户
     * @throws com.backend.backend.exception.InvalidOperationException 如果订单状态不是 PENDING_PAYMENT
     */
    Order markOrderAsPaid(Object identifier, Long userId);

    // TODO: 添加支付成功后更新订单状态的方法
    // boolean processPaymentSuccess(String orderNo);
    // TODO: 添加取票/完成订单的方法
    // boolean completeOrder(String orderNo);

    /**
     * 取消指定时间点之前创建的超时未支付订单，并释放相关座位。
     *
     * @param cutoffTime 超时时间点，此时间点之前创建的订单将被视为超时。
     * @return 成功取消的订单数量。
     */
    int cancelTimedOutPendingOrdersAndReleaseSeats(LocalDateTime cutoffTime);
} 