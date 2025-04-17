package com.backend.backend.service.tmdb;

import com.backend.backend.dto.tmdb.*;
import com.backend.backend.entity.Credit;
import com.backend.backend.entity.Movie;
import com.backend.backend.entity.MovieType;
import com.backend.backend.mapper.MovieMovieTypeMapper;
import com.backend.backend.service.CreditService;
import com.backend.backend.service.MovieService;
import com.backend.backend.service.MovieTypeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 数据填充服务
 * 负责从 TMDB API 获取数据并填充到本地数据库
 */
@Slf4j
@Service
public class DataPopulationService {

    @Autowired
    private TmdbService tmdbService;

    @Autowired
    private MovieService movieService;

    @Autowired
    private MovieTypeService movieTypeService;

    @Autowired
    private CreditService creditService;

    @Autowired
    private MovieMovieTypeMapper movieMovieTypeMapper;

    /**
     * 填充电影类型数据
     * 处理流程：
     * 1. 从 TMDB 获取类型数据并映射到本地 MovieType 实体
     * 2. 如果本地不存在该类型，则保存到数据库
     * 
     * @return Map<Integer, Integer> TMDB类型ID到本地类型ID的映射
     */
    @Transactional
    public Map<Integer, Integer> populateMovieTypes() {
        log.info("开始填充电影类型数据...");
        
        // 获取本地已有的所有电影类型
        List<MovieType> existingTypes = movieTypeService.list();
        Map<Integer, Integer> tmdbTypeIdToLocalIdMap = existingTypes.stream()
                .filter(type -> type.getTmdbGenreId() != null)
                .collect(Collectors.toMap(MovieType::getTmdbGenreId, MovieType::getId));
        
        // 从TMDB获取电影类型数据
        TmdbMovieDetails movieDetails = tmdbService.getMovieDetails(550L); // 使用Fight Club (ID 550) 电影来获取所有类型
        if (movieDetails == null || movieDetails.getGenres() == null) {
            log.error("无法从 TMDB 获取电影类型数据");
            return tmdbTypeIdToLocalIdMap;
        }
        
        List<TmdbGenre> tmdbGenres = movieDetails.getGenres();
        log.info("从 TMDB 获取到 {} 个电影类型", tmdbGenres.size());
        
        // 处理每个TMDB类型
        for (TmdbGenre genre : tmdbGenres) {
            // 如果本地不存在该类型，则保存
            if (!tmdbTypeIdToLocalIdMap.containsKey(genre.getId())) {
                MovieType newType = new MovieType();
                newType.setName(genre.getName());
                newType.setTmdbGenreId(genre.getId());
                
                boolean saved = movieTypeService.save(newType);
                if (saved) {
                    tmdbTypeIdToLocalIdMap.put(genre.getId(), newType.getId());
                    log.info("保存新电影类型: {}, TMDB ID: {}, 本地 ID: {}", genre.getName(), genre.getId(), newType.getId());
                } else {
                    log.error("保存电影类型失败: {}", genre.getName());
                }
            }
        }
        
        log.info("电影类型数据填充完成，共 {} 个类型", tmdbTypeIdToLocalIdMap.size());
        return tmdbTypeIdToLocalIdMap;
    }

    /**
     * 从TMDB API获取电影数据并填充到本地数据库
     * 处理流程：
     * 1. 获取正在上映、即将上映和流行电影列表
     * 2. 对于每部电影，获取详情和演职员信息
     * 3. 映射数据到本地实体并保存
     * 
     * @param maxMoviesPerCategory 每个分类最多获取的电影数量
     * @return 成功导入的电影数量
     */
    @Transactional
    public int populateMoviesFromTmdb(int maxMoviesPerCategory) {
        log.info("开始从TMDB导入电影数据，每类最多 {} 部...", maxMoviesPerCategory);
        
        // 确保电影类型已填充，获取TMDB类型ID到本地ID的映射
        Map<Integer, Integer> tmdbTypeIdToLocalIdMap = populateMovieTypes();
        
        // 获取已有电影的TMDB ID，避免重复导入
        List<Movie> existingMovies = movieService.list();
        Set<Long> existingTmdbIds = existingMovies.stream()
                .filter(movie -> movie.getTmdbId() != null)
                .map(Movie::getTmdbId)
                .collect(Collectors.toSet());
        
        log.info("当前数据库中已有 {} 部电影，其中 {} 部有TMDB ID", existingMovies.size(), existingTmdbIds.size());
        
        int totalImported = 0;
        
        // 获取并导入正在上映的电影
        totalImported += importMoviesByCategory("now_playing", Movie.MovieStatus.NOW_PLAYING, maxMoviesPerCategory, existingTmdbIds, tmdbTypeIdToLocalIdMap);
        
        // 获取并导入即将上映的电影
        totalImported += importMoviesByCategory("upcoming", Movie.MovieStatus.COMING_SOON, maxMoviesPerCategory, existingTmdbIds, tmdbTypeIdToLocalIdMap);
        
        // 获取并导入流行电影 (如果需要的话)
        totalImported += importMoviesByCategory("popular", Movie.MovieStatus.NOW_PLAYING, maxMoviesPerCategory, existingTmdbIds, tmdbTypeIdToLocalIdMap);
        
        log.info("电影数据导入完成，共导入 {} 部电影", totalImported);
        return totalImported;
    }
    
    /**
     * 根据类别导入电影
     * 
     * @param category 类别（now_playing/upcoming/popular）
     * @param status 电影状态
     * @param maxMoviesPerCategory 最大电影数
     * @param existingTmdbIds 已存在的TMDB ID集合
     * @param tmdbTypeIdToLocalIdMap 类型ID映射
     * @return 导入的电影数量
     */
    private int importMoviesByCategory(String category, Movie.MovieStatus status, int maxMoviesPerCategory, 
                                       Set<Long> existingTmdbIds, Map<Integer, Integer> tmdbTypeIdToLocalIdMap) {
        int imported = 0;
        int page = 1;
        
        TmdbMovieListResponse movieList;
        switch (category) {
            case "now_playing":
                movieList = tmdbService.getNowPlayingMovies(page);
                break;
            case "upcoming":
                movieList = tmdbService.getUpcomingMovies(page);
                break;
            case "popular":
                movieList = tmdbService.getPopularMovies(page);
                break;
            default:
                log.error("不支持的电影类别: {}", category);
                return 0;
        }
        
        if (movieList == null || movieList.getResults() == null) {
            log.error("获取{}电影列表失败", category);
            return 0;
        }
        
        log.info("获取到 {} 类别的 {} 部电影", category, movieList.getResults().size());
        
        // 遍历电影列表，处理每一部电影
        for (TmdbMovieResult movieResult : movieList.getResults()) {
            if (imported >= maxMoviesPerCategory) {
                break;
            }
            
            // 检查是否已存在
            if (existingTmdbIds.contains(movieResult.getId())) {
                log.info("电影 {} (TMDB ID: {}) 已存在，跳过", movieResult.getTitle(), movieResult.getId());
                continue;
            }
            
            try {
                // 获取电影详情和演职员信息
                TmdbMovieDetails details = tmdbService.getMovieDetails(movieResult.getId());
                TmdbCreditsResponse credits = tmdbService.getMovieCredits(movieResult.getId());
                
                if (details == null) {
                    log.error("获取电影详情失败: {}", movieResult.getTitle());
                    continue;
                }
                
                // 映射并保存电影信息
                Movie movie = mapTmdbMovieToEntity(details, status);
                boolean saved = movieService.save(movie);
                
                if (!saved) {
                    log.error("保存电影失败: {}", movie.getTitle());
                    continue;
                }
                
                // 保存电影与类型的关联关系
                saveMovieTypeRelations(movie.getId(), details.getGenres(), tmdbTypeIdToLocalIdMap);
                
                // 如果有演职员信息，保存演职员信息
                if (credits != null) {
                    saveMovieCredits(movie.getId(), credits);
                }
                
                existingTmdbIds.add(movie.getTmdbId()); // 添加到已处理集合，防止重复处理
                imported++;
                log.info("成功导入电影: {}, TMDB ID: {}, 状态: {}", movie.getTitle(), movie.getTmdbId(), status);
                
            } catch (Exception e) {
                log.error("处理电影时出错: {}, 错误: {}", movieResult.getTitle(), e.getMessage(), e);
            }
        }
        
        log.info("导入 {} 类别电影完成，共导入 {} 部", category, imported);
        return imported;
    }
    
    /**
     * 将TMDB电影数据转换为本地电影实体
     */
    private Movie mapTmdbMovieToEntity(TmdbMovieDetails details, Movie.MovieStatus status) {
        Movie movie = new Movie();
        movie.setTitle(details.getTitle());
        movie.setTmdbId(details.getId());
        movie.setDescription(details.getOverview());
        
        // 设置海报URL (添加基础URL)
        if (details.getPosterPath() != null && !details.getPosterPath().isEmpty()) {
            movie.setPosterUrl("https://image.tmdb.org/t/p/w500" + details.getPosterPath());
        }
        
        // 解析发行日期
        try {
            if (details.getReleaseDate() != null && !details.getReleaseDate().isEmpty()) {
                movie.setReleaseDate(LocalDate.parse(details.getReleaseDate(), DateTimeFormatter.ISO_DATE));
            }
        } catch (DateTimeParseException e) {
            log.warn("解析电影发行日期失败: {}, 日期: {}", details.getTitle(), details.getReleaseDate());
        }
        
        // 设置制片国家
        if (details.getProductionCountries() != null && !details.getProductionCountries().isEmpty()) {
            movie.setCountry(details.getProductionCountries().stream()
                    .map(TmdbProductionCountry::getName)
                    .collect(Collectors.joining(", ")));
        }
        
        // 设置电影时长
        if (details.getRuntime() != null) {
            movie.setDuration(details.getRuntime());
        }
        
        // 设置评分相关信息
        movie.setTmdbVoteAverage(details.getVoteAverage());
        movie.setTmdbVoteCount(details.getVoteCount());
        movie.setTmdbPopularity(details.getPopularity());
        
        // 初始设置平均评分为TMDB评分
        movie.setAverageRating(details.getVoteAverage());
        
        // 设置总票房为0 (后面可能会更新)
        movie.setTotalBoxOffice(BigDecimal.ZERO);
        
        // 设置电影状态
        movie.setStatus(status);
        
        return movie;
    }
    
    /**
     * 保存电影与类型的关联关系
     */
    private void saveMovieTypeRelations(Long movieId, List<TmdbGenre> genres, Map<Integer, Integer> tmdbTypeIdToLocalIdMap) {
        if (genres == null || genres.isEmpty()) {
            log.warn("电影 ID {} 没有类型信息", movieId);
            return;
        }
        
        // 收集本地类型ID
        List<Integer> typeIds = genres.stream()
                .map(genre -> tmdbTypeIdToLocalIdMap.getOrDefault(genre.getId(), null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        
        if (typeIds.isEmpty()) {
            log.warn("电影 ID {} 的类型在本地不存在", movieId);
            return;
        }
        
        // 先删除已有的关联关系
        movieMovieTypeMapper.deleteByMovieId(movieId);
        
        // 批量插入新的关联关系
        int inserted = movieMovieTypeMapper.insertBatch(movieId, typeIds);
        log.info("电影 ID {} 关联了 {} 个类型", movieId, inserted);
    }
    
    /**
     * 保存电影演职员信息
     */
    private void saveMovieCredits(Long movieId, TmdbCreditsResponse creditsResponse) {
        List<Credit> creditsList = new ArrayList<>();
        
        // 处理导演和主要剧组成员
        if (creditsResponse.getCrew() != null) {
            creditsResponse.getCrew().stream()
                    .filter(crew -> "Director".equals(crew.getJob()) || "Screenplay".equals(crew.getJob()))
                    .forEach(crew -> {
                        Credit credit = new Credit();
                        credit.setMovieId(movieId);
                        credit.setTmdbPersonId(crew.getId().longValue());
                        credit.setTmdbCreditId(crew.getCreditId());
                        credit.setName(crew.getName());
                        credit.setRole(crew.getJob()); // 导演、编剧等
                        
                        if (crew.getProfilePath() != null && !crew.getProfilePath().isEmpty()) {
                            credit.setAvatarUrl("https://image.tmdb.org/t/p/w185" + crew.getProfilePath());
                        }
                        
                        creditsList.add(credit);
                        
                        // 如果是导演，更新电影的导演字段
                        if ("Director".equals(crew.getJob())) {
                            Movie movie = new Movie();
                            movie.setId(movieId);
                            movie.setDirector(crew.getName());
                            movieService.updateById(movie);
                        }
                    });
        }
        
        // 处理主要演员 (最多10个)
        if (creditsResponse.getCast() != null) {
            List<String> actorNames = new ArrayList<>();
            
            creditsResponse.getCast().stream()
                    .limit(10) // 限制演员数量
                    .forEach(cast -> {
                        Credit credit = new Credit();
                        credit.setMovieId(movieId);
                        credit.setTmdbPersonId(cast.getId().longValue());
                        credit.setTmdbCreditId(cast.getCreditId());
                        credit.setName(cast.getName());
                        credit.setRole("Actor"); // 演员
                        credit.setCharacterName(cast.getCharacter());
                        
                        if (cast.getProfilePath() != null && !cast.getProfilePath().isEmpty()) {
                            credit.setAvatarUrl("https://image.tmdb.org/t/p/w185" + cast.getProfilePath());
                        }
                        
                        creditsList.add(credit);
                        actorNames.add(cast.getName());
                    });
            
            // 更新电影的演员字段
            if (!actorNames.isEmpty()) {
                Movie movie = new Movie();
                movie.setId(movieId);
                movie.setActors(String.join(", ", actorNames));
                movieService.updateById(movie);
            }
        }
        
        // 批量保存演职员信息
        if (!creditsList.isEmpty()) {
            boolean saved = creditService.saveBatch(creditsList);
            log.info("电影 ID {} 保存了 {} 个演职员信息, 结果: {}", movieId, creditsList.size(), saved);
        }
    }
}