package com.backend.backend.dto.tmdb;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * TMDB API 电影语言
 */
@Data
public class TmdbSpokenLanguage {
    @JsonProperty("iso_639_1")
    private String iso;
    
    private String name;
    
    @JsonProperty("english_name")
    private String englishName;
} 