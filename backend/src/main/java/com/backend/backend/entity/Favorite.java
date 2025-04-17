package com.backend.backend.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 电影收藏实体 (联合主键 userId, movieId)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("favorite")
public class Favorite implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户ID (主键之一)
     */
    private Long userId;

    /**
     * 电影ID (主键之一)
     */
    private Long movieId;

    /**
     * 收藏时间
     */
    private LocalDateTime createTime;

}
