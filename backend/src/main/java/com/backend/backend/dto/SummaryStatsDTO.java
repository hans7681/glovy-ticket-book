package com.backend.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "管理员仪表盘汇总统计信息 DTO")
public class SummaryStatsDTO {

    @Schema(description = "总用户数")
    private Long totalUsers;

    @Schema(description = "影院总数")
    private Long totalCinemas;

    @Schema(description = "已批准影院数")
    private Long approvedCinemas;

    @Schema(description = "待审核影院数")
    private Long pendingCinemas;

    @Schema(description = "电影总数")
    private Long totalMovies;

    @Schema(description = "订单总数")
    private Long totalOrders;

    @Schema(description = "总销售额 (仅计算已支付订单)")
    private BigDecimal totalRevenue;
}
