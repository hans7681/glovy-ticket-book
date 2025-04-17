package com.backend.backend.mapper;

import com.backend.backend.entity.Rating;
import com.backend.backend.entity.User; // 引入 User
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Optional;

@Mapper
public interface RatingMapper extends BaseMapper<Rating> {

    /**
     * 计算指定电影的平均评分
     *
     * @param movieId 电影ID
     * @return 平均评分，如果无评分则返回 null 或 0.0 (取决于 SQL 实现)
     */
    Double calculateAverageRating(@Param("movieId") Long movieId);

    /**
     * 根据用户ID和电影ID查找评分记录
     * MyBatis Plus Wrapper 也能实现，但这里提供一个显式方法
     * @param userId 用户ID
     * @param movieId 电影ID
     * @return Optional 包含评分记录
     */
    Optional<Rating> findByUserIdAndMovieId(@Param("userId") Long userId, @Param("movieId") Long movieId);

    // 移除自定义的分页方法，将使用 BaseMapper.selectPage 结合 Wrapper
    // Page<Rating> findRatingsWithUserDetailsByMovieId(Page<Rating> page, Long movieId);

     // BaseMapper 已提供 insert, updateById, selectById 等方法
}
