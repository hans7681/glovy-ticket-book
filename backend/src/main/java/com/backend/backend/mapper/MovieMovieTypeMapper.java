package com.backend.backend.mapper;

import com.backend.backend.dto.MovieTypeLinkDTO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.backend.backend.dto.MovieTypeLinkDTO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

// 这个 Mapper 不需要实体类，直接操作关联表
// 不继承 BaseMapper，因为操作比较特殊
@Mapper
public interface MovieMovieTypeMapper {

    @Delete("DELETE FROM movie_movie_type WHERE movie_id = #{movieId}")
    int deleteByMovieId(@Param("movieId") Long movieId);

    // 批量插入关联关系
    // 注意：不同的数据库可能语法不同，这里使用 MySQL 的 VALUES (),()... 语法
    @Insert({
        "<script>",
        "INSERT INTO movie_movie_type (movie_id, movie_type_id) VALUES ",
        "<foreach item='item' index='index' collection='typeIds' separator=',' >",
        "(#{movieId}, #{item})",
        "</foreach>",
        "</script>"
    })
    int insertBatch(@Param("movieId") Long movieId, @Param("typeIds") List<Integer> typeIds);

    /**
     * 根据电影 ID 列表批量查询关联的类型信息。
     * @param movieIds 电影 ID 列表
     * @return 包含电影ID和对应电影类型信息的 DTO 列表
     */
    List<MovieTypeLinkDTO> findTypesForMovies(@Param("ids") List<Long> movieIds);

} 