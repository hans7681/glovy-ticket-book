package com.backend.backend.mapper;

import com.backend.backend.entity.Cinema;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;
import java.util.Map;

@Mapper
public interface CinemaMapper extends BaseMapper<Cinema> {

    // 可以根据需要添加自定义查询，例如根据管理员 ID 查询
    @Select("SELECT * FROM cinema WHERE admin_user_id = #{adminUserId}")
    Cinema findByAdminUserId(@Param("adminUserId") Long adminUserId);

    /**
     * 按状态分组统计影院数量
     * @return List of Map, key "status" is status string, key "count" is the count
     */
    @Select("SELECT status, COUNT(*) as count FROM cinema GROUP BY status")
    List<Map<String, Object>> countCinemasByStatus();

    // BaseMapper 已提供按状态查询等基础功能 (通过 QueryWrapper)
} 