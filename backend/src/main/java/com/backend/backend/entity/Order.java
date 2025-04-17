package com.backend.backend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("t_order") // 使用 t_order 避免 SQL 关键字冲突
public class Order {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField(value = "order_no") // 明确指定列名
    private String orderNo;

    @TableField("user_id")
    private Long userId;

    @TableField("screening_id")
    private Long screeningId;

    @TableField("cinema_id")
    private Long cinemaId; // 冗余

    @TableField("movie_id")
    private Long movieId;  // 冗余

    @TableField("total_amount")
    private BigDecimal totalAmount;

    @TableField("seat_count")
    private Integer seatCount;

    @TableField("status")
    @EnumValue // Store enum name in the database
    private OrderStatus status;

    @TableField("payment_time")
    private LocalDateTime paymentTime;

    @TableField("cancel_time")
    private LocalDateTime cancelTime;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    // --- 非数据库字段 ---
    @TableField(exist = false)
    private List<OrderSeat> seats; // 用于存储关联的座位信息 (查询时填充)
    @TableField(exist = false)
    private Screening screening; // 用于存储关联的场次信息
    @TableField(exist = false)
    private Movie movie; // 用于存储关联的电影信息

    // 定义订单状态枚举
    public enum OrderStatus {
        PENDING_PAYMENT, // 待支付
        PAID,            // 已支付
        CANCELLED,       // 已取消
        REFUNDED,        // 已退款 (预留)
        COMPLETED        // 已完成 (已取票/观影)
    }
} 