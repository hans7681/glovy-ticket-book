package com.backend.backend.mapper;

import com.backend.backend.entity.Movie;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface MovieMapper extends BaseMapper<Movie> {

    /**
     * 根据电影ID查询电影及其关联的类型信息
     * 注意：MyBatis Plus 默认不支持一对多自动映射，需要手写 SQL 或使用 Wrapper
     * 这里为了演示手写 SQL 连接查询
     * @param movieId 电影ID
     * @return 包含类型列表的电影对象
     */
    Movie findMovieWithTypesById(@Param("movieId") Long movieId);

    /**
     * (旧) 分页查询电影列表，包含类型信息
     * 这个方法不再被 Service 层直接调用以解决分页问题
     */
    // Page<Movie> findMoviesWithTypesPage(IPage<Movie> page, @Param("title") String title, @Param("status") String status);

    /**
     * 新增：步骤一 - 分页查询符合条件的电影 ID
     * @param offset 偏移量 (page.offset)
     * @param limit 每页数量 (page.size)
     * @param title 电影标题 (可选查询条件)
     * @param status 电影状态 (可选查询条件)
     * @param movieTypeId 电影类型ID (可选查询条件)
     * @return 当前页的电影 ID 列表
     */
    List<Long> selectMovieIdsPage(@Param("offset") long offset, @Param("limit") long limit,
                                  @Param("title") String title, @Param("status") String status,
                                  @Param("movieTypeId") Integer movieTypeId);

    /**
     * 新增：根据 ID 列表查询电影完整信息（包括类型）
     * @param ids 电影 ID 列表
     * @return 包含类型信息的电影列表
     */
    List<Movie> selectMoviesWithTypesByIds(@Param("ids") List<Long> ids);

    /**
     * 新增：查询符合条件的总电影数量
     * @param title 电影标题 (可选查询条件)
     * @param status 电影状态 (可选查询条件)
     * @param movieTypeId 电影类型ID (可选查询条件)
     * @return 总电影数
     */
    Long countMoviesByCriteria(@Param("title") String title, @Param("status") String status,
                               @Param("movieTypeId") Integer movieTypeId);

    /**
     * (旧的) 分页查询电影，包含所有筛选条件 (使用 MyBatis Plus 拦截器)
     * @param page 分页对象
     * @param title 标题 (模糊查询)
     * @param status 状态
     * @param movieTypeId 类型 ID
     * @return IPage 结果
     */
    // IPage<Movie> selectMoviesPageWithFilters(IPage<Movie> page, @Param("title") String title, @Param("status") String status, @Param("movieTypeId") Integer movieTypeId);

    /**
     * 查询热映口碑榜 (按评分排序)
     * @param limit 返回数量限制
     * @return 电影列表
     */
    List<Movie> findHotPlayingMoviesOrderedByRating(@Param("limit") int limit);

    /**
     * 查询最受期待榜 (按热度排序)
     * @param limit 返回数量限制
     * @return 电影列表
     */
    List<Movie> findUpcomingMoviesOrderedByPopularity(@Param("limit") int limit);

} 