package com.backend.backend.dto.tmdb;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;
import java.util.Date;

/**
 * TMDB API 电影列表项目
 */
@Data
public class TmdbMovieResult {
    private Long id;
    private String title;
    private String overview;
    
    @JsonProperty("poster_path")
    private String posterPath;
    
    @JsonProperty("backdrop_path")
    private String backdropPath;
    
    @JsonProperty("release_date")
    private String releaseDate;
    
    @JsonProperty("genre_ids")
    private List<Integer> genreIds;
    
    @JsonProperty("original_title")
    private String originalTitle;
    
    @JsonProperty("original_language")
    private String originalLanguage;
    
    @JsonProperty("vote_average")
    private Double voteAverage;
    
    @JsonProperty("vote_count")
    private Integer voteCount;
    
    private Double popularity;
    private Boolean adult;
    
    @JsonProperty("video")
    private Boolean hasVideo;
} 