package com.backend.backend.dto.tmdb;

import lombok.Data;
import java.util.List;

/**
 * TMDB API 电影演职员信息响应
 */
@Data
public class TmdbCreditsResponse {
    private Long id;
    private List<TmdbCastMember> cast;
    private List<TmdbCrewMember> crew;
} 