<template>
  <div class="dashboard-view">
    <h2 class="page-header">仪表盘</h2>

    <div v-loading="loadingSummary">
      <!-- Summary Cards -->
      <el-row :gutter="20" class="summary-cards" v-if="summaryStats">
        <el-col :span="6">
          <el-card shadow="hover">
            <div class="stat-title">总用户数</div>
            <div class="stat-value">{{ summaryStats.totalUsers }}</div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover">
            <div class="stat-title">影院总数 (已批准/待审核)</div>
            <div class="stat-value">
              {{ summaryStats.totalCinemas }} ({{ summaryStats.approvedCinemas }}/{{
                summaryStats.pendingCinemas
              }})
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover">
            <div class="stat-title">电影总数</div>
            <div class="stat-value">{{ summaryStats.totalMovies }}</div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover">
            <div class="stat-title">订单总数 / 总销售额</div>
            <div class="stat-value">
              {{ summaryStats.totalOrders }} / {{ formatPrice(summaryStats.totalRevenue) }}
            </div>
          </el-card>
        </el-col>
      </el-row>
      <el-empty v-else-if="!loadingSummary" description="无法加载汇总数据"></el-empty>

      <!-- Charts Row -->
      <el-row :gutter="20" class="charts-row">
        <!-- Order Trend Chart -->
        <el-col :span="12">
          <el-card shadow="hover">
            <template #header>近7日订单趋势</template>
            <div
              ref="orderTrendChartRef"
              class="chart-container"
              v-loading="loadingOrderTrend"
            ></div>
            <el-empty
              v-if="!loadingOrderTrend && !orderTrendData?.length"
              description="暂无订单数据"
            ></el-empty>
          </el-card>
        </el-col>

        <!-- Movie Box Office Chart -->
        <el-col :span="12">
          <el-card shadow="hover">
            <template #header>电影票房排行 (Top 5)</template>
            <div ref="boxOfficeChartRef" class="chart-container" v-loading="loadingBoxOffice"></div>
            <el-empty
              v-if="!loadingBoxOffice && !boxOfficeData?.length"
              description="暂无票房数据"
            ></el-empty>
          </el-card>
        </el-col>
      </el-row>

      <el-row :gutter="20" class="charts-row">
        <!-- Cinema Status Distribution Chart -->
        <el-col :span="12">
          <el-card shadow="hover">
            <template #header>影院状态分布</template>
            <div
              ref="cinemaStatusChartRef"
              class="chart-container"
              v-loading="loadingCinemaStatus"
            ></div>
            <el-empty
              v-if="!loadingCinemaStatus && !cinemaStatusData?.length"
              description="暂无影院数据"
            ></el-empty>
          </el-card>
        </el-col>
        <!-- Placeholder for another chart or element -->
        <el-col :span="12">
          <!-- <el-card shadow="hover">
              <template #header>...</template>
            </el-card> -->
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, shallowRef, nextTick } from 'vue'
import {
  getSummaryStatistics,
  getOrderTrend,
  getTopMovieBoxOffice,
  getCinemaStatusDistribution,
} from '@/services/api'
import type { SummaryStatsDTO, ChartDataPointDTO, StatusDistributionDTO } from '@/types/stats'
import { ElMessage } from 'element-plus'
import { formatPrice } from '@/utils/formatter' // Assuming formatter exists

// Import ECharts
import * as echarts from 'echarts/core'
import {
  TitleComponent,
  TooltipComponent,
  GridComponent,
  LegendComponent,
  ToolboxComponent,
} from 'echarts/components'
import {
  LineChart,
  PieChart,
  BarChart, // Optional: Can use Bar chart for box office too
} from 'echarts/charts'
import { CanvasRenderer } from 'echarts/renderers'
import type { EChartsOption, ECharts } from 'echarts'

// Register ECharts components
echarts.use([
  TitleComponent,
  TooltipComponent,
  GridComponent,
  LegendComponent,
  ToolboxComponent,
  LineChart,
  PieChart,
  BarChart,
  CanvasRenderer,
])

// --- State --- //
const loadingSummary = ref(true)
const loadingOrderTrend = ref(true)
const loadingBoxOffice = ref(true)
const loadingCinemaStatus = ref(true)

const summaryStats = ref<SummaryStatsDTO | null>(null)
const orderTrendData = ref<ChartDataPointDTO[] | null>(null)
const boxOfficeData = ref<ChartDataPointDTO[] | null>(null)
const cinemaStatusData = ref<StatusDistributionDTO[] | null>(null)

// Chart Refs
const orderTrendChartRef = ref<HTMLElement | null>(null)
const boxOfficeChartRef = ref<HTMLElement | null>(null)
const cinemaStatusChartRef = ref<HTMLElement | null>(null)

// Chart Instances (use shallowRef for non-reactive ECharts instances)
const orderTrendChartInstance = shallowRef<ECharts | null>(null)
const boxOfficeChartInstance = shallowRef<ECharts | null>(null)
const cinemaStatusChartInstance = shallowRef<ECharts | null>(null)

// --- Fetch Data --- //

const fetchSummary = async () => {
  loadingSummary.value = true
  try {
    const response = await getSummaryStatistics()
    summaryStats.value = response.data
  } catch (error) {
    console.error('Failed to fetch summary statistics:', error)
    ElMessage.error('加载汇总数据失败')
    summaryStats.value = null // Ensure null on error
  } finally {
    loadingSummary.value = false
  }
}

const fetchOrderTrend = async () => {
  loadingOrderTrend.value = true
  try {
    const response = await getOrderTrend(7) // Fetch last 7 days
    orderTrendData.value = response.data
    await nextTick() // Ensure DOM is updated
    initOrderTrendChart()
  } catch (error) {
    console.error('Failed to fetch order trend:', error)
    ElMessage.error('加载订单趋势失败')
    orderTrendData.value = [] // Ensure empty array on error
  } finally {
    loadingOrderTrend.value = false
  }
}

const fetchBoxOffice = async () => {
  loadingBoxOffice.value = true
  try {
    const response = await getTopMovieBoxOffice(5) // Fetch top 5
    boxOfficeData.value = response.data
    await nextTick()
    initBoxOfficeChart()
  } catch (error) {
    console.error('Failed to fetch box office data:', error)
    ElMessage.error('加载票房排行失败')
    boxOfficeData.value = []
  } finally {
    loadingBoxOffice.value = false
  }
}

const fetchCinemaStatus = async () => {
  loadingCinemaStatus.value = true
  try {
    const response = await getCinemaStatusDistribution()
    cinemaStatusData.value = response.data
    await nextTick()
    initCinemaStatusChart()
  } catch (error) {
    console.error('Failed to fetch cinema status distribution:', error)
    ElMessage.error('加载影院状态分布失败')
    cinemaStatusData.value = []
  } finally {
    loadingCinemaStatus.value = false
  }
}

// --- Init Charts --- //

const initOrderTrendChart = () => {
  if (!orderTrendChartRef.value || !orderTrendData.value) return

  // Dispose existing instance if it exists
  if (orderTrendChartInstance.value) {
    orderTrendChartInstance.value.dispose()
  }

  // Use double assertion
  const chart = echarts.init(orderTrendChartRef.value) as unknown as ECharts
  orderTrendChartInstance.value = chart

  const option: EChartsOption = {
    tooltip: {
      trigger: 'axis',
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true,
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: orderTrendData.value.map((item) => item.name), // Dates
    },
    yAxis: {
      type: 'value',
      name: '订单数',
    },
    series: [
      {
        name: '订单数量',
        type: 'line',
        stack: 'Total', // Optional: if you want area chart style
        areaStyle: {}, // Optional: for area chart
        emphasis: {
          focus: 'series',
        },
        data: orderTrendData.value.map((item) => item.value), // Order counts
      },
    ],
  }

  chart.setOption(option)
}

const initBoxOfficeChart = () => {
  if (!boxOfficeChartRef.value || !boxOfficeData.value) return

  if (boxOfficeChartInstance.value) {
    boxOfficeChartInstance.value.dispose()
  }

  // Use double assertion
  const chart = echarts.init(boxOfficeChartRef.value) as unknown as ECharts
  boxOfficeChartInstance.value = chart

  const option: EChartsOption = {
    tooltip: {
      trigger: 'item',
      formatter: '{a} <br/>{b} : {c} ({d}%)', // Format for pie chart
    },
    legend: {
      orient: 'vertical',
      left: 'left',
      data: boxOfficeData.value.map((item) => item.name), // Movie titles
    },
    series: [
      {
        name: '电影票房',
        type: 'pie',
        radius: '70%', // Adjust size
        center: ['60%', '50%'], // Adjust position
        data: boxOfficeData.value.map((item) => ({
          value: item.value,
          name: item.name,
        })),
        emphasis: {
          itemStyle: {
            shadowBlur: 10,
            shadowOffsetX: 0,
            shadowColor: 'rgba(0, 0, 0, 0.5)',
          },
        },
      },
    ],
  }

  chart.setOption(option)
}

const initCinemaStatusChart = () => {
  if (!cinemaStatusChartRef.value || !cinemaStatusData.value) return

  if (cinemaStatusChartInstance.value) {
    cinemaStatusChartInstance.value.dispose()
  }

  // Use double assertion
  const chart = echarts.init(cinemaStatusChartRef.value) as unknown as ECharts
  cinemaStatusChartInstance.value = chart

  // Map backend status names to readable names if necessary
  const statusMap: { [key: string]: string } = {
    PENDING_APPROVAL: '待审核',
    APPROVED: '已批准',
    REJECTED: '已拒绝',
    DISABLED: '已禁用',
  }

  const formattedData = cinemaStatusData.value.map((item) => ({
    name: statusMap[item.status] || item.status, // Use mapped name or original status
    value: item.count,
  }))

  const option: EChartsOption = {
    tooltip: {
      trigger: 'item',
      formatter: '{a} <br/>{b} : {c} ({d}%)',
    },
    legend: {
      orient: 'vertical',
      left: 'left',
      data: formattedData.map((item) => item.name),
    },
    series: [
      {
        name: '影院状态',
        type: 'pie',
        radius: ['40%', '70%'], // Make it a donut chart
        center: ['60%', '50%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 10,
          borderColor: '#fff',
          borderWidth: 2,
        },
        label: {
          show: false,
          position: 'center',
        },
        emphasis: {
          label: {
            show: true,
            fontSize: '20',
            fontWeight: 'bold',
          },
        },
        labelLine: {
          show: false,
        },
        data: formattedData,
      },
    ],
  }

  chart.setOption(option)
}

// --- Resize Handling --- //
const resizeCharts = () => {
  orderTrendChartInstance.value?.resize()
  boxOfficeChartInstance.value?.resize()
  cinemaStatusChartInstance.value?.resize()
}

// --- Lifecycle Hooks --- //
onMounted(async () => {
  // Fetch all data in parallel
  await Promise.all([fetchSummary(), fetchOrderTrend(), fetchBoxOffice(), fetchCinemaStatus()])

  // Add resize listener
  window.addEventListener('resize', resizeCharts)
})

onBeforeUnmount(() => {
  // Dispose charts
  orderTrendChartInstance.value?.dispose()
  boxOfficeChartInstance.value?.dispose()
  cinemaStatusChartInstance.value?.dispose()
  // Remove listener
  window.removeEventListener('resize', resizeCharts)
})
</script>

<style scoped>
.dashboard-view {
  padding: 20px;
  height: auto; /* 确保视图高度自适应内容 */
  width: 100%;
}

.page-header {
  margin-bottom: 20px;
  font-size: 24px;
  font-weight: 600;
  color: #303133;
}

.summary-cards {
  margin-bottom: 20px;
}

.stat-title {
  font-size: 14px;
  color: #606266;
  margin-bottom: 8px;
}

.stat-value {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
}

.charts-row {
  margin-bottom: 20px;
}

.chart-container {
  height: 300px; /* 图表需要固定高度才能正常显示 */
  width: 100%;
  overflow: hidden; /* 图表容器本身不应滚动 */
}

/* 强制覆盖Element Plus的内部滚动行为 */
:deep(.el-card) {
  height: auto;
  overflow: visible;
}

:deep(.el-card__body) {
  overflow: visible !important;
  padding: 15px;
}

:deep(.el-card__header) {
  padding: 12px 20px;
  font-weight: 600;
  font-size: 16px;
  background-color: #f9f9f9;
  border-bottom: 1px solid #ebeef5;
}

/* 防止图表容器产生滚动 */
:deep(.echarts) {
  overflow: hidden !important;
}

/* 强制覆盖行和列的溢出行为 */
:deep(.el-row),
:deep(.el-col) {
  overflow: visible !important;
}
</style>
