package com.backend.backend.service.impl;

import com.backend.backend.entity.Favorite;
import com.backend.backend.entity.Movie;
import com.backend.backend.exception.ResourceNotFoundException;
import com.backend.backend.mapper.FavoriteMapper;
import com.backend.backend.service.FavoriteService;
import com.backend.backend.service.MovieService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class FavoriteServiceImpl implements FavoriteService {

    private static final Logger log = LoggerFactory.getLogger(FavoriteServiceImpl.class);

    @Autowired
    private FavoriteMapper favoriteMapper;

    @Autowired
    private MovieService movieService; // 用于检查电影是否存在

    @Override
    @Transactional
    public boolean addFavorite(Long userId, Long movieId) {
        // 1. 检查电影是否存在
        Movie movie = movieService.getById(movieId);
        if (movie == null) {
            log.warn("Attempted to favorite non-existent movie with ID: {}", movieId);
            throw new ResourceNotFoundException("电影不存在，无法收藏。 MovieId: " + movieId);
        }

        // 2. 尝试插入收藏记录
        Favorite favorite = new Favorite(userId, movieId, LocalDateTime.now());
        try {
            int inserted = favoriteMapper.insert(favorite);
            // insert 方法返回值是影响的行数。如果 ON DUPLICATE KEY UPDATE 生效，可能返回 1 或 2 (取决于 MySQL 版本和配置)
            // 如果是新插入，通常返回 1。如果已存在且 ON DUPLICATE KEY 触发，可能返回 1 或 0 或 2
            // 为了简化，我们检查是否已存在来判断是否是 *新* 添加
             if (inserted > 0) {
                 log.info("User {} favorited movie {}", userId, movieId);
                 return true; // 表示操作成功执行（无论是新插入还是已存在）
             } else {
                 // 如果 insert 返回 0，可能是因为 ON DUPLICATE KEY UPDATE user_id=user_id 没有实际更新行
                 // 我们再查一次确认是否已存在
                 return isFavorite(userId, movieId);
             }
             // 或者更严谨地判断是否是 *新添加* 的：先查，不存在再插。但性能稍差。
             // Optional<Favorite> existing = favoriteMapper.findByUserIdAndMovieId(userId, movieId);
             // if (existing.isPresent()) {
             //     log.info("Movie {} was already favorited by user {}", movieId, userId);
             //     return false; // 表示之前已收藏
             // }
             // int inserted = favoriteMapper.insert(favorite);
             // if (inserted > 0) {
             //     log.info("User {} favorited movie {}", userId, movieId);
             //     return true; // 表示新添加成功
             // }
        } catch (Exception e) {
            // 处理可能的数据库异常，例如外键约束失败（理论上 movieService.getById 已处理）
            log.error("Error adding favorite for user {} and movie {}: {}", userId, movieId, e.getMessage());
            // 可以根据具体异常类型决定是否抛出或返回 false
            return false;
        }
    }

    @Override
    @Transactional
    public boolean removeFavorite(Long userId, Long movieId) {
        int deleted = favoriteMapper.deleteByUserIdAndMovieId(userId, movieId);
        if (deleted > 0) {
            log.info("User {} unfavorited movie {}", userId, movieId);
        } else {
            log.warn("Attempted to unfavorite movie {} by user {}, but it was not favorited.", movieId, userId);
        }
        return deleted > 0; // 返回是否真的删除了记录
    }

    @Override
    public boolean isFavorite(Long userId, Long movieId) {
        // 使用 count(*) > 0 来判断是否存在
        return favoriteMapper.checkFavoriteStatus(userId, movieId) > 0;
        // 或者使用 findByUserIdAndMovieId().isPresent()
        // return favoriteMapper.findByUserIdAndMovieId(userId, movieId).isPresent();
    }

    @Override
    public Page<Movie> getFavoriteMovies(Page<Movie> page, Long userId) {
        // 1. 查询总数 (这个方法没有分页冲突，可以保留)
        long total = favoriteMapper.countFavoriteMoviesByUserId(userId);
        if (total <= 0) {
            return new Page<>(page.getCurrent(), page.getSize(), 0);
        }

        // 2. 计算手动分页参数
        long offset = page.offset(); // page.getCurrent() - 1) * page.getSize();
        long limit = page.getSize();

        // 3. 调用手动分页的 Mapper 方法获取当前页数据
        List<Movie> favoriteMovies = favoriteMapper.findFavoriteMoviesByUserIdManualPaging(userId, offset, limit);

        // 4. 手动设置 Page 对象
        Page<Movie> resultPage = new Page<>(page.getCurrent(), page.getSize(), total);
        resultPage.setRecords(favoriteMovies);
        // resultPage.setPages(page.getPages()); // 总页数会自动计算

        return resultPage;

        // 原来的方法调用 (会导致错误)
        // return favoriteMapper.findFavoriteMoviesByUserId(page, userId);
    }
}
