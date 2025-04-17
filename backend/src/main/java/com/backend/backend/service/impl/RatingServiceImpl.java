package com.backend.backend.service.impl;

import com.backend.backend.dto.RatingRequestDTO;
import com.backend.backend.entity.Movie;
import com.backend.backend.entity.Rating;
import com.backend.backend.entity.User;
import com.backend.backend.exception.ResourceNotFoundException;
import com.backend.backend.mapper.RatingMapper;
import com.backend.backend.mapper.UserMapper;
import com.backend.backend.mapper.RatingMapper;
import com.backend.backend.service.MovieService;
import com.backend.backend.service.RatingService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.Valid;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Validated
public class RatingServiceImpl extends ServiceImpl<RatingMapper, Rating> implements RatingService {

    private static final Logger log = LoggerFactory.getLogger(RatingServiceImpl.class);

    @Autowired
    private RatingMapper ratingMapper;

    @Autowired
    private MovieService movieService;

    @Autowired
    private UserMapper userMapper;

    @Override
    @Transactional
    public Rating addOrUpdateRating(Long userId, Long movieId, @Valid RatingRequestDTO ratingRequest) {
        Movie movie = movieService.getById(movieId);
        if (movie == null) {
            log.warn("Attempted to rate non-existent movie with ID: {}", movieId);
            throw new ResourceNotFoundException("电影不存在，无法评分。 MovieId: " + movieId);
        }

        Optional<Rating> existingRatingOpt = ratingMapper.findByUserIdAndMovieId(userId, movieId);

        Rating ratingToSave;
        if (existingRatingOpt.isPresent()) {
            ratingToSave = existingRatingOpt.get();
            ratingToSave.setScore(ratingRequest.getScore());
            ratingToSave.setComment(ratingRequest.getComment());
            ratingToSave.setUpdateTime(LocalDateTime.now());
            boolean updated = this.updateById(ratingToSave);
             if (updated) {
                 log.info("User {} updated rating for movie {}", userId, movieId);
             } else {
                 log.warn("Failed to update rating for user {} and movie {}", userId, movieId);
             }
        } else {
            ratingToSave = new Rating();
            ratingToSave.setUserId(userId);
            ratingToSave.setMovieId(movieId);
            ratingToSave.setScore(ratingRequest.getScore());
            ratingToSave.setComment(ratingRequest.getComment());
            ratingToSave.setCreateTime(LocalDateTime.now());
            ratingToSave.setUpdateTime(LocalDateTime.now());
            boolean saved = this.save(ratingToSave);
             if (saved) {
                 log.info("User {} added a new rating for movie {}", userId, movieId);
             } else {
                 log.error("Failed to save new rating for user {} and movie {}", userId, movieId);
             }
        }

        if (ratingToSave.getId() != null) {
             updateMovieAverageRating(movieId);
        }

        return ratingToSave;
    }

    @Override
    public Rating getMyRatingForMovie(Long userId, Long movieId) {
        return ratingMapper.findByUserIdAndMovieId(userId, movieId).orElse(null);
    }

    @Override
    public Page<Rating> getRatingsForMovie(Page<Rating> page, Long movieId) {
        QueryWrapper<Rating> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("movie_id", movieId);
        queryWrapper.orderByDesc("update_time");

        Page<Rating> ratingPage = baseMapper.selectPage(page, queryWrapper);

        if (ratingPage != null && !CollectionUtils.isEmpty(ratingPage.getRecords())) {
            List<Long> userIds = ratingPage.getRecords().stream()
                                           .map(Rating::getUserId)
                                           .distinct()
                                           .collect(Collectors.toList());

            if (!CollectionUtils.isEmpty(userIds)) {
                QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
                userQueryWrapper.in("id", userIds);
                userQueryWrapper.select("id", "nickname", "avatar");
                List<User> users = userMapper.selectList(userQueryWrapper);

                Map<Long, User> userMap = users.stream()
                                               .collect(Collectors.toMap(User::getId, u -> u));

                ratingPage.getRecords().forEach(rating -> {
                    User userInfo = userMap.get(rating.getUserId());
                    if (userInfo != null) {
                        User safeUserInfo = new User();
                        safeUserInfo.setId(userInfo.getId());
                        safeUserInfo.setNickname(userInfo.getNickname());
                        safeUserInfo.setAvatar(userInfo.getAvatar());
                        rating.setUser(safeUserInfo);
                    }
                });
            }
        }

        return ratingPage;
    }

    @Override
    @Transactional
    public void updateMovieAverageRating(Long movieId) {
        Double averageScore = ratingMapper.calculateAverageRating(movieId);
        if (averageScore == null) {
            averageScore = 0.0;
        }
        double roundedAverage = Math.round(averageScore * 10.0) / 10.0;

        Movie movieToUpdate = new Movie();
        movieToUpdate.setId(movieId);
        movieToUpdate.setAverageRating(roundedAverage);
        boolean updated = movieService.updateById(movieToUpdate);
        if (updated) {
             log.info("Successfully updated average rating for movie {} to {}", movieId, roundedAverage);
        } else {
             log.warn("Failed to update average rating for movie {}. Movie might not exist.", movieId);
        }
    }
}
