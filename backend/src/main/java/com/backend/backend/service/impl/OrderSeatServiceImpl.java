package com.backend.backend.service.impl;

import com.backend.backend.entity.OrderSeat;
import com.backend.backend.mapper.OrderSeatMapper;
import com.backend.backend.service.OrderSeatService;
import com.backend.backend.dto.SeatIdentifier;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
public class OrderSeatServiceImpl extends ServiceImpl<OrderSeatMapper, OrderSeat> implements OrderSeatService {

    @Autowired
    private OrderSeatMapper orderSeatMapper;

    @Override
    public List<OrderSeat> findSeatsByScreeningId(Long screeningId) {
        LambdaQueryWrapper<OrderSeat> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderSeat::getScreeningId, screeningId);
        return orderSeatMapper.selectList(wrapper);
    }

    @Override
    public boolean areSeatsSold(Long screeningId, List<SeatIdentifier> seatsToCheck) {
        if (seatsToCheck == null || seatsToCheck.isEmpty()) {
            return false;
        }
        LambdaQueryWrapper<OrderSeat> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderSeat::getScreeningId, screeningId);
        // 使用 IN 或者 OR 构建查询条件
        wrapper.and(w -> {
            for (int i = 0; i < seatsToCheck.size(); i++) {
                SeatIdentifier seat = seatsToCheck.get(i);
                 w.or(inner -> inner.eq(OrderSeat::getRowIndex, seat.getRowIndex())
                                     .eq(OrderSeat::getColIndex, seat.getColIndex()));
            }
        });
        // 只需要检查是否存在，不需要获取完整列表
        return orderSeatMapper.selectCount(wrapper) > 0;
    }

    @Override
    public List<OrderSeat> findByOrderId(Long orderId) {
        LambdaQueryWrapper<OrderSeat> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderSeat::getOrderId, orderId);
        return orderSeatMapper.selectList(wrapper);
    }

    @Override
    @Transactional
    public int deleteByOrderId(Long orderId) {
        LambdaQueryWrapper<OrderSeat> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderSeat::getOrderId, orderId);
        return orderSeatMapper.delete(wrapper);
    }
} 