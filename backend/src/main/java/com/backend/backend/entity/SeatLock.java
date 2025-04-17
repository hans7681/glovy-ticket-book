package com.backend.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("seat_lock")
public class SeatLock {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long screeningId;

    private Integer rowIndex;

    private Integer colIndex;

    private Long userId;

    private LocalDateTime lockExpiryTime;

    private LocalDateTime createTime;
} 