package com.backend.backend.dto.tmdb;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

/**
 * TMDB API 电影详情
 */
@Data
public class TmdbMovieDetails {
    private Long id;
    private String title;
    private String overview;
    private List<TmdbGenre> genres;
    
    @JsonProperty("poster_path")
    private String posterPath;
    
    @JsonProperty("backdrop_path")
    private String backdropPath;
    
    @JsonProperty("release_date")
    private String releaseDate;
    
    private Integer runtime;
    private String status;
    
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
    
    @JsonProperty("imdb_id")
    private String imdbId;
    
    private String tagline;
    
    @JsonProperty("production_companies")
    private List<TmdbProductionCompany> productionCompanies;
    
    @JsonProperty("production_countries")
    private List<TmdbProductionCountry> productionCountries;
    
    @JsonProperty("spoken_languages")
    private List<TmdbSpokenLanguage> spokenLanguages;
    
    private Long budget;
    private Long revenue;
} 