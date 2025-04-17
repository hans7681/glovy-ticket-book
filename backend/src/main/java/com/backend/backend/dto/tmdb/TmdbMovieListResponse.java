package com.backend.backend.dto.tmdb;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

/**
 * TMDB API 电影列表响应
 */
@Data
public class TmdbMovieListResponse {
    private int page;
    private List<TmdbMovieResult> results;
    
    @JsonProperty("total_results")
    private int totalResults;
    
    @JsonProperty("total_pages")
    private int totalPages;
} 