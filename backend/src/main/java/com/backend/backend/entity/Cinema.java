package com.backend.backend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("cinema")
public class Cinema {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;
    private String address;
    private String phone;
    private String logo;
    private String description;

    private Long adminUserId; // 关联的影院管理员用户ID

    @TableField(value = "status")
    private CinemaStatus status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    // 定义影院状态枚举
    public enum CinemaStatus {
        PENDING_APPROVAL, // 待审核
        APPROVED,         // 已批准
        REJECTED,         // 已拒绝
        DISABLED          // 已禁用
    }
} 