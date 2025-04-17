package com.backend.backend.service;

import com.backend.backend.entity.Movie;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface MovieService extends IService<Movie> {

    /**
     * 添加电影，并处理与类型的关联关系
     * @param movie 包含 movieTypeIds 的电影对象
     * @return 是否成功
     */
    boolean saveMovieWithTypes(Movie movie);

    /**
     * 更新电影，并处理与类型的关联关系
     * @param movie 包含 movieTypeIds 的电影对象
     * @return 是否成功
     */
    boolean updateMovieWithTypes(Movie movie);

    /**
     * 根据 ID 获取电影详情，包含类型信息
     * @param id 电影 ID
     * @return 电影对象，如果不存在则返回 null
     */
    Movie getMovieWithTypesById(Long id);

    /**
     * 分页获取电影列表，包含类型信息，并支持按类型 ID 筛选
     * @param page 分页参数
     * @param title 查询条件：标题
     * @param status 查询条件：状态
     * @param movieTypeId 查询条件：电影类型 ID (可选)
     * @return 分页结果
     */
    Page<Movie> listMoviesWithTypes(Page<Movie> page, String title, String status, Integer movieTypeId);

    /**
     * 分页获取电影列表，包含类型信息，并支持按类型 ID 筛选
     * @param page 分页参数
     * @param title 查询条件：标题
     * @param status 查询条件：状态
     * @param movieTypeId 查询条件：电影类型 ID (可选)
     * @return 分页结果
     */
    // Page<Movie> listMoviesWithFilters(Page<Movie> page, String title, String status, Integer movieTypeId);

    /**
     * 删除电影，并处理关联关系 (示例：仅删除 movie_movie_type)
     * @param id 电影 ID
     * @return 是否成功
     */
    boolean deleteMovieWithRelations(Long id);

    /**
     * 获取热映口碑榜 (按评分排序)
     * @param limit 返回数量限制
     * @return 电影列表
     */
    List<Movie> getHotPlayingMoviesRankedByRating(int limit);

    /**
     * 获取最受期待榜 (按热度排序)
     * @param limit 返回数量限制
     * @return 电影列表
     */
    List<Movie> getUpcomingMoviesRankedByPopularity(int limit);
} 