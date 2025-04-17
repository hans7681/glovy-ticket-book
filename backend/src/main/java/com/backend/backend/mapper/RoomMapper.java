package com.backend.backend.mapper;

import com.backend.backend.entity.Room;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
 
@Mapper
public interface RoomMapper extends BaseMapper<Room> {
    // BaseMapper 提供了基础 CRUD
    // 通常查询会基于 cinemaId，可以使用 QueryWrapper 实现
} 