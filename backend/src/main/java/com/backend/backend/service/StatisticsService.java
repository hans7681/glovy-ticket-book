package com.backend.backend.service;

import com.backend.backend.dto.ChartDataPointDTO;
import com.backend.backend.dto.StatusDistributionDTO;
import com.backend.backend.dto.SummaryStatsDTO;

import java.util.List;

/**
 * 提供管理员仪表盘统计数据的服务接口
 */
public interface StatisticsService {

    /**
     * 获取汇总统计信息 (指标卡片)
     * @return 汇总统计 DTO
     */
    SummaryStatsDTO getSummaryStats();

    /**
     * 获取指定天数内的订单趋势数据 (按天分组)
     * @param days 查询的天数 (例如 7 或 30)
     * @return 包含日期和订单数量的数据点列表
     */
    List<ChartDataPointDTO> getOrderTrend(int days);

    /**
     * 获取票房收入排名前 N 的电影数据
     * @param limit 要获取的电影数量
     * @return 包含电影标题和票房收入的数据点列表
     */
    List<ChartDataPointDTO> getTopMovieBoxOffice(int limit);

    /**
     * 获取影院状态分布数据
     * @return 包含状态名称和数量的数据点列表
     */
    List<StatusDistributionDTO> getCinemaStatusDistribution();

}
