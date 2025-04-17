package com.backend.backend.mapper;

import com.backend.backend.entity.Screening;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ScreeningMapper extends BaseMapper<Screening> {

    /**
     * 检查指定影厅在给定时间段内是否存在已批准或待审批的场次冲突
     * 冲突条件：新场次的开始时间或结束时间落在已有场次的时间段内，
     * 或者新场次的时间段完全包含已有场次。
     * (status = 'APPROVED' OR status = 'PENDING_APPROVAL') AND roomId = #{roomId} AND
     * (
     *   (#{startTime} >= start_time AND #{startTime} < end_time) OR -- 新开始时间在旧场次内
     *   (#{endTime} > start_time AND #{endTime} <= end_time) OR   -- 新结束时间在旧场次内
     *   (#{startTime} <= start_time AND #{endTime} >= end_time)    -- 新场次包含旧场次
     * )
     * AND id != #{excludeScreeningId} -- 排除自身 (用于更新检查)
     *
     * @param roomId 影厅 ID
     * @param startTime 新场次的开始时间
     * @param endTime 新场次的结束时间
     * @param excludeScreeningId 要排除的场次ID (更新时传入当前场次ID，新增时传入 null 或 0)
     * @return 冲突的场次列表
     */
    @Select({
        "<script>",
        "SELECT * FROM screening",
        "WHERE room_id = #{roomId}",
        "AND status IN ('APPROVED', 'PENDING_APPROVAL')", // 只检查已批准和待审批的
        "AND (",
        "  (start_time &lt; #{endTime} AND end_time &gt; #{startTime})", // 简化后的冲突判断：两个时间段有交集
        ")",
        "<if test='excludeScreeningId != null and excludeScreeningId != 0'>",
        "  AND id != #{excludeScreeningId}",
        "</if>",
        "LIMIT 1", // 找到一个冲突即可返回
        "</script>"
    })
    List<Screening> findConflictingScreenings(@Param("roomId") Long roomId,
                                              @Param("startTime") LocalDateTime startTime,
                                              @Param("endTime") LocalDateTime endTime,
                                              @Param("excludeScreeningId") Long excludeScreeningId);

    /**
     * 根据电影 ID 查询未来最早的已批准场次的开始时间
     * @param movieId 电影 ID
     * @return 最早的开始时间 (LocalDateTime)，如果找不到则返回 null
     */
    LocalDateTime findEarliestApprovedScreeningTime(@Param("movieId") Long movieId);

} 