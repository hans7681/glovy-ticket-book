package com.backend.backend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("screening")
public class Screening {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long movieId;
    private Long roomId;
    private Long cinemaId; // 冗余字段，方便查询

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    private BigDecimal price;

    @TableField(value = "status")
    private ScreeningStatus status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    // 定义场次状态枚举
    public enum ScreeningStatus {
        PENDING_APPROVAL, // 待审批
        APPROVED,         // 已批准
        REJECTED,         // 已拒绝
        FINISHED,         // 已结束 (可以通过定时任务或查询时判断 endTime)
        CANCELLED         // 已取消
    }
} 