package com.backend.backend.service;

import com.backend.backend.entity.Movie;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

public interface FavoriteService {

    /**
     * 添加电影到用户收藏
     *
     * @param userId  用户ID
     * @param movieId 电影ID
     * @return 是否成功添加 (如果已收藏则返回 false 或 true，取决于业务逻辑，这里返回是否是新添加的)
     * @throws com.backend.backend.exception.ResourceNotFoundException 如果电影不存在
     */
    boolean addFavorite(Long userId, Long movieId);

    /**
     * 从用户收藏中移除电影
     *
     * @param userId  用户ID
     * @param movieId 电影ID
     * @return 是否成功移除 (如果原本未收藏则返回 false)
     */
    boolean removeFavorite(Long userId, Long movieId);

    /**
     * 检查用户是否收藏了某电影
     *
     * @param userId  用户ID
     * @param movieId 电影ID
     * @return true 如果已收藏，false 如果未收藏
     */
    boolean isFavorite(Long userId, Long movieId);

    /**
     * 获取用户收藏的电影列表 (分页)
     *
     * @param page   分页参数
     * @param userId 用户ID
     * @return 包含收藏电影信息的分页结果
     */
    Page<Movie> getFavoriteMovies(Page<Movie> page, Long userId);

}
