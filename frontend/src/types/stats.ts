/**
 * DTO for summary statistics on the admin dashboard.
 * GET /api/admin/stats/summary
 */
export interface SummaryStatsDTO {
  totalUsers: number // 总用户数
  totalCinemas: number // 影院总数
  approvedCinemas: number // 已批准影院数
  pendingCinemas: number // 待审核影院数
  totalMovies: number // 电影总数
  totalOrders: number // 订单总数
  totalRevenue: number // 总销售额 (仅计算已支付订单)
}

/**
 * Generic data point DTO for charts (name/label and value).
 * Used for order trend and movie box office.
 * GET /api/admin/stats/order-trend
 * GET /api/admin/stats/movie-box-office
 */
export interface ChartDataPointDTO {
  name: string // Data point name (e.g., date 'YYYY-MM-DD' or movie title)
  value: number // Data point value (e.g., order count or box office amount)
}

/**
 * DTO for status distribution data point.
 * Used for cinema status distribution.
 * GET /api/admin/stats/cinema-status-distribution
 */
export interface StatusDistributionDTO {
  status: string // Status name (e.g., 'APPROVED', 'PENDING_APPROVAL')
  count: number // Count for this status
}
