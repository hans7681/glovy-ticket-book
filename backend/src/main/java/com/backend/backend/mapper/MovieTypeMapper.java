package com.backend.backend.mapper;

import com.backend.backend.entity.MovieType;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MovieTypeMapper extends BaseMapper<MovieType> {
    // BaseMapper 已经提供了常用的 CRUD 方法
    // 如果需要自定义 SQL 可以在这里添加，例如复杂的连接查询
} 