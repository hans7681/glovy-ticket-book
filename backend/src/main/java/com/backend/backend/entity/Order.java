package com.backend.backend.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/**
 * 订单实体类
 */
@Data
@TableName("t_order")
public class Order {

    /**
     * 订单ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 订单号
     */
    @TableField("order_no")
    private String orderNo;

    /**
     * 用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 场次ID
     */
    @TableField("screening_id")
    private Long screeningId;

    /**
     * 影院ID
     */
    @TableField("cinema_id")
    private Long cinemaId;

    /**
     * 电影ID
     */
    @TableField("movie_id")
    private Long movieId;

    /**
     * 订单总金额
     */
    @TableField("total_amount")
    private BigDecimal totalAmount;

    /**
     * 座位数量
     */
    @TableField("seat_count")
    private Integer seatCount;

    /**
     * 订单状态（PENDING_PAYMENT：待支付，PAID：已支付，CANCELLED：已取消，REFUNDED：已退款，COMPLETED：已完成）
     */
    @TableField("status")
    @EnumValue
    private OrderStatus status;

    /**
     * 支付时间
     */
    @TableField("payment_time")
    private LocalDateTime paymentTime;

    /**
     * 取消时间
     */
    @TableField("cancel_time")
    private LocalDateTime cancelTime;

    /**
     * 创建时间
     */
    @TableField("create_time")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField("update_time")
    private LocalDateTime updateTime;

    // --- 非数据库字段 ---
    @TableField(exist = false)
    private List<OrderSeat> seats; // 用于存储关联的座位信息 (查询时填充)
    @TableField(exist = false)
    private Screening screening; // 用于存储关联的场次信息
    @TableField(exist = false)
    private Movie movie; // 用于存储关联的电影信息

    /**
     * 支付宝交易号
     */
    @TableField(exist = false) // 如果数据库中没有此字段，暂时标记为非数据库字段
    private String alipayTradeNo;
    
    /**
     * 前端回调URL (支付完成后跳转的前端页面地址)
     */
    @TableField(exist = false)
    private String frontendCallbackUrl;

    // 定义订单状态枚举
    public enum OrderStatus {
        PENDING_PAYMENT, // 待支付
        PAID,            // 已支付
        CANCELLED,       // 已取消
        REFUNDED,        // 已退款 (预留)
        COMPLETED        // 已完成 (已取票/观影)
    }
} 