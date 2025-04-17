package com.backend.backend.dto.tmdb;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * TMDB API 制片公司
 */
@Data
public class TmdbProductionCompany {
    private Integer id;
    private String name;
    
    @JsonProperty("logo_path")
    private String logoPath;
    
    @JsonProperty("origin_country")
    private String originCountry;
} 