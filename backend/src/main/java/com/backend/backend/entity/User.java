package com.backend.backend.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.FieldFill;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("user") // 明确指定表名
public class User {
    @TableId(type = IdType.AUTO) // 明确是自增ID
    private Long id;
    private String username;
    private String password;
    private String nickname;
    private String avatar;
    private String phone;
    private String email;
    private UserRole role;
    
    @TableField(fill = FieldFill.INSERT) // 插入时自动填充
    private LocalDateTime createTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE) // 插入和更新时自动填充
    private LocalDateTime updateTime;

    // 定义用户角色枚举
    public enum UserRole {
        USER, CINEMA_ADMIN, SYSTEM_ADMIN
    }
} 