package com.backend.backend.dto.tmdb;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * TMDB API 演员
 */
@Data
public class TmdbCastMember {
    private Integer id;
    private String name;
    private Integer order;
    private String character;
    
    @JsonProperty("profile_path")
    private String profilePath;
    
    @JsonProperty("credit_id")
    private String creditId;
    
    @JsonProperty("known_for_department")
    private String knownForDepartment;
    
    private Boolean adult;
    private Integer gender;
    
    @JsonProperty("original_name")
    private String originalName;
    
    private Double popularity;
    
    @JsonProperty("cast_id")
    private Integer castId;
} 