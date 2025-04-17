package com.backend.backend.service.tmdb;

import com.backend.backend.dto.tmdb.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * TMDB API 服务
 * 负责与 TMDB API 进行交互，获取电影数据
 */
@Service
public class TmdbService {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    @Autowired
    public TmdbService(RestTemplate restTemplate,
                       @Value("${tmdb.api.base-url:https://api.themoviedb.org/3}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    /**
     * 获取正在上映的电影
     * @param page 页码
     * @param language 语言 (默认为英语)
     * @return 电影列表响应
     */
    public TmdbMovieListResponse getNowPlayingMovies(int page, String language) {
        String url = UriComponentsBuilder.fromUriString(baseUrl)
                .path("/movie/now_playing")
                .queryParam("page", page)
                .queryParam("language", language)
                .build()
                .toUriString();
        
        return restTemplate.getForObject(url, TmdbMovieListResponse.class);
    }
    
    /**
     * 获取即将上映的电影
     * @param page 页码
     * @param language 语言 (默认为英语)
     * @return 电影列表响应
     */
    public TmdbMovieListResponse getUpcomingMovies(int page, String language) {
        String url = UriComponentsBuilder.fromUriString(baseUrl)
                .path("/movie/upcoming")
                .queryParam("page", page)
                .queryParam("language", language)
                .build()
                .toUriString();
        
        return restTemplate.getForObject(url, TmdbMovieListResponse.class);
    }
    
    /**
     * 获取流行电影
     * @param page 页码
     * @param language 语言 (默认为英语)
     * @return 电影列表响应
     */
    public TmdbMovieListResponse getPopularMovies(int page, String language) {
        String url = UriComponentsBuilder.fromUriString(baseUrl)
                .path("/movie/popular")
                .queryParam("page", page)
                .queryParam("language", language)
                .build()
                .toUriString();
        
        return restTemplate.getForObject(url, TmdbMovieListResponse.class);
    }
    
    /**
     * 获取电影详情
     * @param movieId 电影ID
     * @param language 语言 (默认为英语)
     * @return 电影详情
     */
    public TmdbMovieDetails getMovieDetails(Long movieId, String language) {
        String url = UriComponentsBuilder.fromUriString(baseUrl)
                .path("/movie/{id}")
                .queryParam("language", language)
                .buildAndExpand(movieId)
                .toUriString();
        
        return restTemplate.getForObject(url, TmdbMovieDetails.class);
    }
    
    /**
     * 获取电影演职员信息
     * @param movieId 电影ID
     * @param language 语言 (默认为英语)
     * @return 电影演职员信息
     */
    public TmdbCreditsResponse getMovieCredits(Long movieId, String language) {
        String url = UriComponentsBuilder.fromUriString(baseUrl)
                .path("/movie/{id}/credits")
                .queryParam("language", language)
                .buildAndExpand(movieId)
                .toUriString();
        
        return restTemplate.getForObject(url, TmdbCreditsResponse.class);
    }
    
    // 重载方法，使用默认语言 (中文)
    public TmdbMovieListResponse getNowPlayingMovies(int page) {
        return getNowPlayingMovies(page, "zh-CN");
    }
    
    public TmdbMovieListResponse getUpcomingMovies(int page) {
        return getUpcomingMovies(page, "zh-CN");
    }
    
    public TmdbMovieListResponse getPopularMovies(int page) {
        return getPopularMovies(page, "zh-CN");
    }
    
    public TmdbMovieDetails getMovieDetails(Long movieId) {
        return getMovieDetails(movieId, "zh-CN");
    }
    
    public TmdbCreditsResponse getMovieCredits(Long movieId) {
        return getMovieCredits(movieId, "zh-CN");
    }
} 