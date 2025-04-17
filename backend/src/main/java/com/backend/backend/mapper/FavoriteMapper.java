package com.backend.backend.mapper;

import com.backend.backend.entity.Favorite;
import com.backend.backend.entity.Movie;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface FavoriteMapper {

    /**
     * 插入一条收藏记录
     *
     * @param favorite 收藏记录 (包含 userId 和 movieId)
     * @return 影响的行数
     */
    int insert(Favorite favorite);

    /**
     * 根据用户ID和电影ID删除收藏记录
     *
     * @param userId  用户ID
     * @param movieId 电影ID
     * @return 影响的行数
     */
    int deleteByUserIdAndMovieId(@Param("userId") Long userId, @Param("movieId") Long movieId);

    /**
     * 根据用户ID和电影ID查找收藏记录
     *
     * @param userId  用户ID
     * @param movieId 电影ID
     * @return Optional 包含收藏记录 (如果存在)
     */
    Optional<Favorite> findByUserIdAndMovieId(@Param("userId") Long userId, @Param("movieId") Long movieId);

    /**
     * 分页查询用户收藏的电影列表
     * 注意：这里返回的是 Movie 对象列表，需要联表查询
     *
     * @param page   分页对象，MyBatis Plus 会自动处理分页逻辑
     * @param userId 用户ID
     * @return 包含电影信息的分页结果
     */
    Page<Movie> findFavoriteMoviesByUserId(Page<Movie> page, @Param("userId") Long userId);

    /**
     * 统计用户收藏的电影总数
     * @param userId 用户ID
     * @return 收藏总数
     */
    long countFavoriteMoviesByUserId(@Param("userId") Long userId);

    /**
     * (可选) 检查用户是否收藏了某个电影
     * @param userId 用户ID
     * @param movieId 电影ID
     * @return 如果收藏了返回大于0的数，否则返回0
     */
     int checkFavoriteStatus(@Param("userId") Long userId, @Param("movieId") Long movieId);

    /**
     * (手动分页) 查询用户收藏的电影列表
     *
     * @param userId 用户ID
     * @param offset 偏移量
     * @param limit  数量
     * @return 电影列表
     */
    List<Movie> findFavoriteMoviesByUserIdManualPaging(@Param("userId") Long userId, @Param("offset") long offset, @Param("limit") long limit);

}
