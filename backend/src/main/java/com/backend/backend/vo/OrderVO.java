package com.backend.backend.vo;

import com.backend.backend.entity.Order;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "订单详情视图对象")
public class OrderVO {

    @Schema(description = "订单ID")
    private Long orderId; // Use orderId to avoid confusion with entity's id

    @Schema(description = "订单号")
    private String orderNo;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "场次ID")
    private Long screeningId;

    @Schema(description = "电影ID")
    private Long movieId;

    @Schema(description = "电影标题")
    private String movieTitle;

    @Schema(description = "影院ID")
    private Long cinemaId;

    @Schema(description = "影院名称")
    private String cinemaName;

    @Schema(description = "影厅名称")
    private String roomName;

    @Schema(description = "放映开始时间")
    private LocalDateTime screeningStartTime;

    @Schema(description = "总金额")
    private BigDecimal totalAmount;

    @Schema(description = "座位数量")
    private Integer seatCount;

    @Schema(description = "订单状态")
    private Order.OrderStatus status;

    @Schema(description = "支付时间")
    private LocalDateTime paymentTime;

    @Schema(description = "取消时间")
    private LocalDateTime cancelTime;

    @Schema(description = "订单创建时间")
    private LocalDateTime createTime;

    @Schema(description = "座位描述列表 (例如: ['5排7座', '5排8座'])")
    private List<String> seatsDescription;

    // 可以根据需要添加更多信息，如电影海报、影院地址等

} 