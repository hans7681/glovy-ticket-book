package com.backend.backend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

/**
 * 电影演职员实体类
 */
@Data
@TableName("credit")
public class Credit {

    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long movieId; // 关联电影ID
    
    private Long tmdbPersonId; // TMDB 的人物 ID
    
    private String tmdbCreditId; // TMDB 的 credit ID (标识电影中的演职条目)
    
    private String name; // 演职人员姓名
    
    private String role; // 角色 (如 导演, 主演, 配角)
    
    private String characterName; // 饰演角色名 (如果是演员)
    
    private String avatarUrl; // 头像 URL (可能来自 TMDB profile_path)
} 