package com.backend.backend.dto;

import com.backend.backend.entity.MovieType;
import lombok.Data;

@Data
public class MovieTypeLinkDTO {
    private Long movieId;
    private MovieType movieType; // 直接嵌套 MovieType 实体
}
