package com.backend.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

@Data
@TableName("order_seat")
public class OrderSeat implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("order_id")
    private Long orderId;

    @TableField("screening_id") // Redundant but potentially useful
    private Long screeningId;

    @TableField("row_index")
    private Integer rowIndex;

    @TableField("col_index")
    private Integer colIndex;

    // Optional: A human-readable label like "5排8座"
    // This could be generated on the fly or stored if needed frequently
    @TableField("seat_label")
    private String seatLabel;
} 