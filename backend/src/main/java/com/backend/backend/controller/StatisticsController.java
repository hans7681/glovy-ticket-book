package com.backend.backend.controller;

import com.backend.backend.dto.ChartDataPointDTO;
import com.backend.backend.dto.StatusDistributionDTO;
import com.backend.backend.dto.SummaryStatsDTO;
import com.backend.backend.service.StatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "管理员 - 统计信息", description = "提供管理员仪表盘所需的统计数据接口 (需要系统管理员权限)")
@RestController
@RequestMapping("/api/admin/stats") // Base path for statistics endpoints
@PreAuthorize("hasRole('SYSTEM_ADMIN')") // Secure all endpoints in this controller
public class StatisticsController {

    @Autowired
    private StatisticsService statisticsService;

    @GetMapping("/summary")
    @Operation(summary = "获取汇总统计信息", description = "获取仪表盘顶部指标卡片的汇总数据。")
    @ApiResponse(responseCode = "200", description = "成功获取汇总统计",
                 content = @Content(mediaType = "application/json", schema = @Schema(implementation = SummaryStatsDTO.class)))
    public ResponseEntity<SummaryStatsDTO> getSummaryStatistics() {
        SummaryStatsDTO stats = statisticsService.getSummaryStats();
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/order-trend")
    @Operation(summary = "获取订单趋势", description = "获取指定天数内的每日订单数量趋势图数据。")
    @ApiResponse(responseCode = "200", description = "成功获取订单趋势数据",
                 content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = ChartDataPointDTO.class))))
    public ResponseEntity<List<ChartDataPointDTO>> getOrderTrend(
            @Parameter(description = "查询的天数 (例如 7 或 30)", example = "7")
            @RequestParam(defaultValue = "7") int days) {
        List<ChartDataPointDTO> trendData = statisticsService.getOrderTrend(days);
        return ResponseEntity.ok(trendData);
    }

    @GetMapping("/movie-box-office")
    @Operation(summary = "获取电影票房排行", description = "获取票房收入排名前 N 的电影及其票房数据，用于饼图或条形图展示。")
    @ApiResponse(responseCode = "200", description = "成功获取电影票房排行数据",
                 content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = ChartDataPointDTO.class))))
    public ResponseEntity<List<ChartDataPointDTO>> getTopMovieBoxOffice(
            @Parameter(description = "要获取的电影数量 (例如 5 或 10)", example = "5")
            @RequestParam(defaultValue = "5") int limit) {
        List<ChartDataPointDTO> boxOfficeData = statisticsService.getTopMovieBoxOffice(limit);
        return ResponseEntity.ok(boxOfficeData);
    }

    @GetMapping("/cinema-status-distribution")
    @Operation(summary = "获取影院状态分布", description = "获取不同状态（待审核、已批准等）的影院数量分布数据，用于饼图展示。")
    @ApiResponse(responseCode = "200", description = "成功获取影院状态分布数据",
                 content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = StatusDistributionDTO.class))))
    public ResponseEntity<List<StatusDistributionDTO>> getCinemaStatusDistribution() {
        List<StatusDistributionDTO> distributionData = statisticsService.getCinemaStatusDistribution();
        return ResponseEntity.ok(distributionData);
    }
}
