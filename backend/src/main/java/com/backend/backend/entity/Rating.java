package com.backend.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 电影评分实体
 */
@Data
@TableName("rating")
public class Rating implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 评分ID (自增主键)
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    @NotNull // 确保用户ID不为空
    private Long userId;

    /**
     * 电影ID
     */
    @NotNull // 确保电影ID不为空
    private Long movieId;

    /**
     * 评分 (1-10)
     * 使用 TINYINT 存储，对应 Java 的 Byte 或 Short，这里用 Integer 并加校验
     */
    @NotNull(message = "评分不能为空")
    @Min(value = 1, message = "评分不能低于1")
    @Max(value = 10, message = "评分不能高于10")
    private Integer score; // 使用 Integer 便于校验，数据库是 TINYINT

    /**
     * 评论内容 (可选)
     */
    private String comment;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    // --- DTO/VO 可能需要的额外字段 ---
    /**
     * (非数据库字段) 关联的用户信息 (用于展示)
     */
    private transient User user; // 使用 transient 关键字避免序列化和数据库映射

}
