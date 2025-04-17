package com.backend.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("movie_type") // 对应数据库表名
public class MovieType {

    @TableId(type = IdType.AUTO) // 指定主键自增
    private Integer id;

    private String name;

    private Integer tmdbGenreId; // 对应 tmdb_genre_id
} 