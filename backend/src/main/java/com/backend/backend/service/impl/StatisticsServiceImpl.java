package com.backend.backend.service.impl;

import com.backend.backend.dto.ChartDataPointDTO;
import com.backend.backend.dto.StatusDistributionDTO;
import com.backend.backend.dto.SummaryStatsDTO;
import com.backend.backend.entity.Movie; // Import Movie entity
import com.backend.backend.mapper.*;
import com.backend.backend.service.StatisticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils; // Import CollectionUtils

import java.util.Collections; // Import Collections
import java.util.Map;
import java.util.List;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

@Service
public class StatisticsServiceImpl implements StatisticsService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private CinemaMapper cinemaMapper;

    @Autowired
    private MovieMapper movieMapper;

    @Autowired
    private OrderMapper orderMapper;

    // Define formatter if needed
     private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE; // YYYY-MM-DD

    @Override
    public SummaryStatsDTO getSummaryStats() {
        Long totalUsers = userMapper.selectCount(null);
        Long totalMovies = movieMapper.selectCount(null);
        Long totalOrders = orderMapper.countTotalOrders(); // Use custom method or selectCount
        BigDecimal totalRevenue = orderMapper.sumTotalPaidRevenue();

        // Get cinema status counts
        List<Map<String, Object>> cinemaStatusCounts = cinemaMapper.countCinemasByStatus();
        long totalCinemas = 0L;
        long approvedCinemas = 0L;
        long pendingCinemas = 0L;
        for (Map<String, Object> statusCount : cinemaStatusCounts) {
            String status = (String) statusCount.get("status");
            Long count = (Long) statusCount.get("count");
            totalCinemas += count;
            if ("APPROVED".equals(status)) {
                approvedCinemas = count;
            } else if ("PENDING_APPROVAL".equals(status)) {
                pendingCinemas = count;
            }
        }

        return new SummaryStatsDTO(totalUsers, totalCinemas, approvedCinemas, pendingCinemas, totalMovies, totalOrders, totalRevenue);
    }

    @Override
    public List<ChartDataPointDTO> getOrderTrend(int days) {
        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = endDate.minusDays(days);

        List<Map<String, Object>> trendData = orderMapper.countOrdersGroupedByDate(startDate, endDate);

        return trendData.stream()
                .map(data -> new ChartDataPointDTO(
                        (String) data.get("order_date"),
                        new BigDecimal((Long) data.get("count")) // Order count as BigDecimal
                ))
                .collect(Collectors.toList());
    }

    @Override
    public List<ChartDataPointDTO> getTopMovieBoxOffice(int limit) {
        List<Map<String, Object>> boxOfficeData = orderMapper.sumPaidRevenueGroupedByMovie(limit);

        if (CollectionUtils.isEmpty(boxOfficeData)) {
            return Collections.emptyList();
        }

        // Extract movie IDs
        List<Long> movieIds = boxOfficeData.stream()
                .map(data -> (Long) data.get("movie_id"))
                .collect(Collectors.toList());

        // Fetch movie titles
        Map<Long, String> movieTitleMap = movieMapper.selectBatchIds(movieIds).stream()
                .collect(Collectors.toMap(Movie::getId, Movie::getTitle));

        // Combine data
        return boxOfficeData.stream()
                .map(data -> new ChartDataPointDTO(
                        movieTitleMap.getOrDefault((Long) data.get("movie_id"), "未知电影"), // Use movie title as name
                        (BigDecimal) data.get("total_revenue") // Revenue as value
                ))
                .collect(Collectors.toList());
    }

    @Override
    public List<StatusDistributionDTO> getCinemaStatusDistribution() {
        List<Map<String, Object>> statusData = cinemaMapper.countCinemasByStatus();

        return statusData.stream()
                .map(data -> new StatusDistributionDTO(
                        (String) data.get("status"),
                        (Long) data.get("count")
                ))
                .collect(Collectors.toList());
    }
}
