package com.backend.backend.dto.tmdb;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * TMDB API 制片国家
 */
@Data
public class TmdbProductionCountry {
    @JsonProperty("iso_3166_1")
    private String iso;
    
    private String name;
} 