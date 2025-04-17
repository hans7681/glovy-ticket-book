package com.backend.backend.service;

import com.backend.backend.dto.RatingRequestDTO;
import com.backend.backend.entity.Rating;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;

@Validated
public interface RatingService extends IService<Rating> {

    /**
     * 添加或更新用户对电影的评分
     *
     * @param userId 用户ID
     * @param movieId 电影ID
     * @param ratingRequest 包含评分 (score) 和评论 (comment) 的请求 DTO
     * @return 保存或更新后的 Rating 对象
     * @throws com.backend.backend.exception.ResourceNotFoundException 如果电影不存在
     * @throws jakarta.validation.ConstraintViolationException 如果评分无效 (例如超出范围)
     */
    Rating addOrUpdateRating(Long userId, Long movieId, @Valid RatingRequestDTO ratingRequest);

    /**
     * 获取用户对特定电影的评分信息
     *
     * @param userId 用户ID
     * @param movieId 电影ID
     * @return Rating 对象，如果用户未评分则返回 null
     */
    Rating getMyRatingForMovie(Long userId, Long movieId);

    /**
     * 分页获取电影的评分列表 (包含用户信息)
     *
     * @param page 分页参数
     * @param movieId 电影ID
     * @return 包含评分和用户信息的 Page 对象
     */
    Page<Rating> getRatingsForMovie(Page<Rating> page, Long movieId);

    /**
     * 重新计算并更新电影的平均评分
     * 可以在 addOrUpdateRating 后调用，或者通过其他机制触发 (如定时任务、消息队列)
     * @param movieId 电影ID
     */
    void updateMovieAverageRating(Long movieId);

    // ... (其他方法保持不变)

}
