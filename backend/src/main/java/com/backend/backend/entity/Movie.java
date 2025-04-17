package com.backend.backend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List; // 用于接收关联类型ID

@Data
@TableName("movie")
public class Movie {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;
    private String director;
    private String actors; // 可以保持为 String，或改为 List<String> 并使用类型处理器
    private Integer duration;
    private String description;
    private String posterUrl; // 对应 poster_url
    private LocalDate releaseDate; // 对应 release_date
    private String country;

    @TableField(value = "status") // 明确指定数据库字段名，如果和属性名不完全一致
    private MovieStatus status;

    private String trailerUrl; // 对应 trailer_url
    private Long tmdbId; // 对应 tmdb_id
    private Double tmdbVoteAverage; // 对应 tmdb_vote_average
    private Integer tmdbVoteCount; // 对应 tmdb_vote_count
    private Double tmdbPopularity; // 对应 tmdb_popularity
    private BigDecimal totalBoxOffice; // 对应 total_box_office
    private Double averageRating; // 对应 average_rating

    @TableField(fill = FieldFill.INSERT) // 插入时自动填充
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE) // 插入和更新时自动填充
    private LocalDateTime updateTime;

    // --- 非数据库字段 ---
    @TableField(exist = false) // 标记为非数据库字段
    private List<Integer> movieTypeIds; // 用于接收前端传来的关联类型ID列表

    @TableField(exist = false)
    private List<MovieType> movieTypes; // 用于存储关联的类型信息 (查询时填充)

    // 定义电影状态枚举
    public enum MovieStatus {
        COMING_SOON, NOW_PLAYING, OFFLINE
    }
} 