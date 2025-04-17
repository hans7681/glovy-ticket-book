package com.backend.backend.service;

import com.backend.backend.entity.OrderSeat;
import com.backend.backend.dto.SeatIdentifier;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface OrderSeatService extends IService<OrderSeat> {

    /**
     * 查询指定场次的所有已售座位记录
     * @param screeningId 场次ID
     * @return 已售座位列表
     */
    List<OrderSeat> findSeatsByScreeningId(Long screeningId);

    /**
     * 检查指定场次的某些座位是否已被售出
     * @param screeningId 场次ID
     * @param seatsToCheck 需要检查的座位列表 (使用 dto.SeatIdentifier)
     * @return 如果有任何一个座位被售出，则返回 true，否则返回 false
     */
    boolean areSeatsSold(Long screeningId, List<SeatIdentifier> seatsToCheck);

    /**
     * 根据订单 ID 查询关联的所有座位记录
     * @param orderId 订单 ID
     * @return 订单座位列表
     */
    List<OrderSeat> findByOrderId(Long orderId);

    /**
     * 根据订单 ID 删除关联的所有座位记录
     * @param orderId 订单 ID
     * @return 删除的记录数
     */
    int deleteByOrderId(Long orderId);
} 