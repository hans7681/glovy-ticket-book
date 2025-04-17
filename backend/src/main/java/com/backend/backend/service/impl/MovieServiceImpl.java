package com.backend.backend.service.impl;

import com.backend.backend.entity.Movie;
import com.backend.backend.mapper.MovieMapper;
import com.backend.backend.mapper.MovieMovieTypeMapper;
import com.backend.backend.service.MovieService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MovieServiceImpl extends ServiceImpl<MovieMapper, Movie> implements MovieService {

    @Autowired
    private MovieMovieTypeMapper movieMovieTypeMapper;

    @Autowired // 或者直接使用 baseMapper
    private MovieMapper movieMapper;

    @Override
    @Transactional // 开启事务管理
    public boolean saveMovieWithTypes(Movie movie) {
        // 1. 保存电影基本信息 (调用 ServiceImpl 的 save 方法)
        boolean movieSaved = this.save(movie); // 保存后 movie 对象会获得 id
        if (!movieSaved || movie.getId() == null) {
            return false;
        }

        // 2. 处理电影与类型的关联关系
        List<Integer> typeIds = movie.getMovieTypeIds();
        if (!CollectionUtils.isEmpty(typeIds)) {
            // 校验类型 ID 数量 (1-3个)
            if (typeIds.size() > 3) {
                 throw new RuntimeException("一个电影最多只能关联3个类型");
            }
            movieMovieTypeMapper.insertBatch(movie.getId(), typeIds);
        }

        return true;
    }

    @Override
    @Transactional
    public boolean updateMovieWithTypes(Movie movie) {
        // 1. 更新电影基本信息
        boolean movieUpdated = this.updateById(movie);
        if (!movieUpdated) {
            return false;
        }

        // 2. 更新电影与类型的关联关系 (先删后插)
        Long movieId = movie.getId();
        movieMovieTypeMapper.deleteByMovieId(movieId);

        List<Integer> typeIds = movie.getMovieTypeIds();
        if (!CollectionUtils.isEmpty(typeIds)) {
            // 校验类型 ID 数量 (1-3个)
            if (typeIds.size() > 3) {
                 throw new RuntimeException("一个电影最多只能关联3个类型");
            }
            movieMovieTypeMapper.insertBatch(movieId, typeIds);
        }
        return true;
    }

    @Override
    public Movie getMovieWithTypesById(Long id) {
        // 调用 Mapper 中的自定义方法
        return movieMapper.findMovieWithTypesById(id);
    }

    @Override
    public Page<Movie> listMoviesWithTypes(Page<Movie> page, String title, String status, Integer movieTypeId) {
        // 1. 查询总记录数 (不包含类型关联)
        long total = movieMapper.countMoviesByCriteria(title, status, movieTypeId);
        if (total <= 0) {
            // 如果没有符合条件的电影，直接返回空 Page 对象
            return new Page<>(page.getCurrent(), page.getSize(), 0);
        }

        // 2. 计算分页参数
        long offset = page.offset();
        long limit = page.getSize();

        // 3. 查询当前页的电影 ID 列表
        List<Long> currentMovieIds = movieMapper.selectMovieIdsPage(offset, limit, title, status, movieTypeId);

        List<Movie> movieRecords = Collections.emptyList();
        if (!CollectionUtils.isEmpty(currentMovieIds)) {
            // 4. 根据 ID 列表查询电影详细信息 (包含类型)
             movieRecords = movieMapper.selectMoviesWithTypesByIds(currentMovieIds);

             // (可选的健壮性处理) 确保返回的顺序与 ID 列表顺序一致
             Map<Long, Movie> movieMap = movieRecords.stream()
                     .collect(Collectors.toMap(Movie::getId, m -> m, (existing, replacement) -> existing)); // 保留第一个出现的
             movieRecords = currentMovieIds.stream()
                     .map(movieMap::get)
                     .filter(java.util.Objects::nonNull)
                     .collect(Collectors.toList());

        }

        // 5. 组装并返回 Page 对象
        Page<Movie> resultPage = new Page<>(page.getCurrent(), page.getSize(), total);
        resultPage.setRecords(movieRecords);

        return resultPage;
    }

    @Override
    @Transactional
    public boolean deleteMovieWithRelations(Long id) {
        // 1. 删除电影与类型的关联关系
        movieMovieTypeMapper.deleteByMovieId(id);

        // 2. 删除电影本身
        boolean movieDeleted = this.removeById(id);

        // TODO: 处理其他关联表的外键约束，例如：
        // creditMapper.deleteByMovieId(id);
        // favoriteMapper.deleteByMovieId(id);
        // ratingMapper.deleteByMovieId(id);
        // screeningMapper.deleteByMovieId(id); // 需要考虑级联删除或逻辑删除策略

        return movieDeleted;
    }

    /*
    @Override
    public Page<Movie> listMoviesWithFilters(Page<Movie> page, String title, String status, Integer movieTypeId) {
        // 1. 调用 Mapper 层的新方法执行分页查询 (返回 IPage)
        IPage<Movie> queryResultPage = movieMapper.selectMoviesPageWithFilters(page, title, status, movieTypeId);

        // 2. 创建并返回 Page<Movie> 对象
        if (queryResultPage == null) {
            return new Page<>(page.getCurrent(), page.getSize()); // Return empty page
        } else {
            // 从 IPage 结果创建 Page 对象
            Page<Movie> finalPage = new Page<>(queryResultPage.getCurrent(), queryResultPage.getSize(), queryResultPage.getTotal());
            finalPage.setRecords(queryResultPage.getRecords());
            finalPage.setPages(queryResultPage.getPages()); // Copy pages count
            // finalPage.setOrders(queryResultPage.getOrders()); // Copy orders if needed
            // finalPage.setOptimizeCountSql(queryResultPage.optimizeCountSql()); // etc.
            return finalPage;
        }
    }
    */

    @Override
    public List<Movie> getHotPlayingMoviesRankedByRating(int limit) {
        return movieMapper.findHotPlayingMoviesOrderedByRating(limit);
    }

    @Override
    public List<Movie> getUpcomingMoviesRankedByPopularity(int limit) {
        return movieMapper.findUpcomingMoviesOrderedByPopularity(limit);
    }
} 