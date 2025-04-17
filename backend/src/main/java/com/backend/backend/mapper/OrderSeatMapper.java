package com.backend.backend.mapper;

import com.backend.backend.entity.OrderSeat;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface OrderSeatMapper extends BaseMapper<OrderSeat> {

    /**
     * 检查指定场次的特定座位是否已被锁定 (查询是否存在记录)
     * @param screeningId 场次 ID
     * @param rowIndex 行号
     * @param colIndex 列号
     * @return 如果座位已被锁定，返回对应的 OrderSeat 记录；否则返回 null。
     *   注意：这里可以加上 FOR UPDATE 来实现悲观锁，防止并发问题。
     *   例如：SELECT * FROM order_seat WHERE screening_id = #{screeningId} AND row_index = #{rowIndex} AND col_index = #{colIndex} FOR UPDATE
     */
    @Select("SELECT * FROM order_seat WHERE screening_id = #{screeningId} AND row_index = #{rowIndex} AND col_index = #{colIndex}")
    OrderSeat findLockedSeat(@Param("screeningId") Long screeningId, @Param("rowIndex") Integer rowIndex, @Param("colIndex") Integer colIndex);

    /**
     * 根据订单 ID 删除所有关联的座位记录
     * @param orderId 订单 ID
     * @return 删除的行数
     */
    @Delete("DELETE FROM order_seat WHERE order_id = #{orderId}")
    int deleteByOrderId(@Param("orderId") Long orderId);

    /**
     * 根据订单 ID 查询所有关联的座位记录
     * @param orderId 订单 ID
     * @return
     */
    @Select("SELECT * FROM order_seat WHERE order_id = #{orderId}")
    List<OrderSeat> findByOrderId(@Param("orderId") Long orderId);

    /**
     * Batch insert order seats.
     * Assumes your MyBatis XML mapping configuration supports this.
     * Example XML snippet:
     * <insert id="insertBatch">
     *     INSERT INTO t_order_seat (order_id, screening_id, row_index, col_index) VALUES
     *     <foreach collection="list" item="item" separator=",">
     *         (#{item.orderId}, #{item.screeningId}, #{item.rowIndex}, #{item.colIndex})
     *     </foreach>
     * </insert>
     *
     * @param orderSeats List of OrderSeat entities to insert.
     * @return Number of rows affected.
     */
    int insertBatch(@Param("list") List<OrderSeat> orderSeats);

    /**
     * 根据订单ID列表批量删除座位记录。
     *
     * @param orderIds 订单ID列表。
     * @return 删除的行数。
     */
    int deleteSeatsByOrderIds(@Param("orderIds") List<Long> orderIds);

} 