package com.backend.backend.mapper;

import com.backend.backend.entity.Order;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 订单数据访问接口
 */
@Mapper
public interface OrderMapper extends BaseMapper<Order> {
    // BaseMapper 提供基础 CRUD
    // 可能需要自定义查询，例如关联查询订单座位、场次、电影信息

    /**
     * 统计订单总数
     * @return 订单总数
     */
    Long countTotalOrders(); // 可以使用 selectCount(null)

    /**
     * 计算总销售额 (仅计算 PAID 状态的订单)
     * @return 总销售额
     */
    BigDecimal sumTotalPaidRevenue();

    /**
     * 按日期分组统计指定时间范围内的订单数量 (仅 PAID)
     * @param startDate 开始时间 (包含)
     * @param endDate 结束时间 (不包含)
     * @return List of Map, key "order_date" is date string (YYYY-MM-DD), key "count" is the count
     */
    List<Map<String, Object>> countOrdersGroupedByDate(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * 按电影分组统计已支付订单的总销售额，并按销售额降序排序
     * @param limit 限制返回的电影数量
     * @return List of Map, key "movie_id" is movie ID, key "total_revenue" is the sum
     */
    List<Map<String, Object>> sumPaidRevenueGroupedByMovie(@Param("limit") int limit);

    /**
     * 查询指定时间点之前创建的待支付订单的ID列表。
     *
     * @param cutoffTime 时间点。
     * @return 超时的待支付订单ID列表。
     */
    List<Long> findTimedOutPendingOrderIds(@Param("cutoffTime") LocalDateTime cutoffTime);

    /**
     * 批量更新订单状态为已取消。
     *
     * @param orderIds 要取消的订单ID列表。
     * @param cancelTime 取消时间。
     * @return 更新的行数。
     */
    int batchCancelOrders(@Param("orderIds") List<Long> orderIds, @Param("cancelTime") LocalDateTime cancelTime);

    /**
     * 根据订单号查询订单
     * 
     * @param orderNo 订单号
     * @return 订单信息
     */
    @Select("SELECT * FROM t_order WHERE order_no = #{orderNo}")
    Order selectByOrderNo(@Param("orderNo") String orderNo);
} 