package com.backend.backend.mapper;

import com.backend.backend.entity.Announcement;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AnnouncementMapper extends BaseMapper<Announcement> {
    // BaseMapper 提供基础 CRUD
} 