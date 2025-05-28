<template>
  <div class="cinema-admin-orders-view">
    <div class="page-header">
      <h2>影院订单管理</h2>
    </div>

    <!-- Filters -->
    <el-card class="filter-card" shadow="never">
      <el-form :inline="true" :model="filters" @submit.prevent="handleFilter" class="filter-form">
        <el-form-item label="订单状态">
          <el-select
            v-model="filters.status"
            placeholder="所有状态"
            clearable
            @change="handleFilter"
            style="width: 150px"
          >
            <el-option
              v-for="option in orderStatusOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
        </el-form-item>
        <!-- Add more filters if needed (e.g., date range) -->
        <!--
        <el-form-item label="日期范围">
          <el-date-picker
            v-model="filters.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            @change="handleFilter"
          />
        </el-form-item>
        -->
        <el-form-item>
          <el-button type="primary" @click="handleFilter">
            <el-icon><Search /></el-icon> 查询
          </el-button>
          <el-button @click="handleResetFilters">
            <el-icon><Refresh /></el-icon> 重置
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- Orders Table -->
    <el-card class="table-card" shadow="never">
      <div class="table-header">
        <div class="table-title">订单列表</div>
      </div>
      <el-table :data="orders" v-loading="loading" stripe border style="width: 100%">
        <el-table-column prop="orderNo" label="订单号" width="200" show-overflow-tooltip />
        <el-table-column prop="userId" label="用户ID" width="100" align="center" />
        <el-table-column prop="movieTitle" label="电影" min-width="150" show-overflow-tooltip />
        <el-table-column prop="roomName" label="影厅" width="120" />
        <el-table-column prop="screeningStartTime" label="放映时间" width="170" align="center">
          <template #default="{ row }">
            {{ formatDateTime(row.screeningStartTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="seatCount" label="座位数" width="80" align="center" />
        <el-table-column prop="totalAmount" label="总金额" width="100" align="right">
          <template #default="{ row }"> ¥{{ row.totalAmount?.toFixed(2) ?? '?.??' }} </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusTagType(row.status)">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="下单时间" width="170" align="center">
          <template #default="{ row }">
            {{ formatDateTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleViewDetails(row)">
              查看详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- Pagination -->
      <el-pagination
        v-if="pagination.total > 0"
        background
        layout="total, sizes, prev, pager, next, jumper"
        :total="pagination.total"
        :current-page="pagination.currentPage"
        :page-size="pagination.pageSize"
        :page-sizes="[10, 20, 50, 100]"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
        class="pagination-container"
      />
    </el-card>

    <!-- Order Detail Dialog -->
    <el-dialog v-model="detailDialogVisible" title="订单详情" width="700px" top="5vh" center>
      <div v-loading="detailLoading">
        <el-descriptions v-if="selectedOrder" :column="2" border>
          <el-descriptions-item label="订单号">{{ selectedOrder.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="用户ID">{{ selectedOrder.userId }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getStatusTagType(selectedOrder.status)">
              {{ getStatusText(selectedOrder.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="电影">{{ selectedOrder.movieTitle }}</el-descriptions-item>
          <el-descriptions-item label="影院">{{ selectedOrder.cinemaName }}</el-descriptions-item>
          <el-descriptions-item label="影厅">{{ selectedOrder.roomName }}</el-descriptions-item>
          <el-descriptions-item label="放映时间">{{
            formatDateTime(selectedOrder.screeningStartTime)
          }}</el-descriptions-item>
          <el-descriptions-item label="座位数量">{{
            selectedOrder.seatCount
          }}</el-descriptions-item>
          <el-descriptions-item label="总金额"
            >¥{{ selectedOrder.totalAmount?.toFixed(2) ?? '?.??' }}</el-descriptions-item
          >
          <el-descriptions-item label="下单时间">{{
            formatDateTime(selectedOrder.createTime)
          }}</el-descriptions-item>
          <el-descriptions-item label="支付时间">{{
            formatDateTime(selectedOrder.paymentTime)
          }}</el-descriptions-item>
          <el-descriptions-item label="取消时间">{{
            formatDateTime(selectedOrder.cancelTime)
          }}</el-descriptions-item>
          <el-descriptions-item label="座位详情" :span="2">
            <span
              v-if="selectedOrder.seatsDescription && selectedOrder.seatsDescription.length > 0"
            >
              {{ selectedOrder.seatsDescription.join(', ') }}
            </span>
            <span v-else>无座位信息</span>
          </el-descriptions-item>
        </el-descriptions>
        <el-empty v-else description="无法加载订单详情" />
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="detailDialogVisible = false">关闭</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import {
  ElCard,
  ElForm,
  ElFormItem,
  ElSelect,
  ElOption,
  ElButton,
  ElTable,
  ElTableColumn,
  ElPagination,
  ElDialog,
  ElDescriptions,
  ElDescriptionsItem,
  ElTag,
  ElMessage,
  ElEmpty,
} from 'element-plus'
import type { TagProps } from 'element-plus'
import { listCinemaOrders, getCinemaOrderDetails } from '@/services/api' // Use correct import path
import type { OrderVO } from '@/types/order'
import type { Page } from '@/types/page'
import { formatDateTime } from '@/utils/datetime'
import { Search, Refresh } from '@element-plus/icons-vue'

// Interface for API parameters - ensure status matches API expectations (enum or undefined)
interface ListCinemaOrdersParams {
  current?: number
  size?: number
  status?: 'PENDING_PAYMENT' | 'PAID' | 'CANCELLED' | 'REFUNDED' | 'COMPLETED' // No empty string here
}

// Interface for local filters - allow empty string for UI select
interface LocalFilters {
  status: 'PENDING_PAYMENT' | 'PAID' | 'CANCELLED' | 'REFUNDED' | 'COMPLETED' | ''
}

// Helper type for Axios errors (similar to previous component)
interface AxiosErrorWithResponse {
  response?: {
    data?: {
      message?: string
    }
  }
}
function isAxiosErrorWithResponse(error: unknown): error is AxiosErrorWithResponse {
  return typeof error === 'object' && error !== null && 'response' in error
}

// --- Component State ---
const loading = ref(false)
const detailLoading = ref(false)
const orders = ref<OrderVO[]>([])
const pagination = reactive({
  currentPage: 1,
  pageSize: 10,
  total: 0,
})
// Use LocalFilters type for reactive filters
const filters = reactive<LocalFilters>({
  status: '',
})
const orderStatusOptions = [
  { value: 'PENDING_PAYMENT', label: '待支付' },
  { value: 'PAID', label: '已支付' },
  { value: 'CANCELLED', label: '已取消' },
  { value: 'REFUNDED', label: '已退款' },
  { value: 'COMPLETED', label: '已完成' },
]
const selectedOrder = ref<OrderVO | null>(null)
const detailDialogVisible = ref(false)

// --- Data Fetching Logic ---
const fetchOrders = async () => {
  loading.value = true
  try {
    // Prepare params for API call, converting empty string status to undefined
    const params: ListCinemaOrdersParams = {
      current: pagination.currentPage,
      size: pagination.pageSize,
      // Use ternary operator for clarity: if filters.status is empty string, pass undefined, else pass the status
      status: filters.status ? filters.status : undefined,
    }
    const response = await listCinemaOrders(params)
    const pageData: Page<OrderVO> = response.data
    orders.value = pageData.records || []
    pagination.total = pageData.total || 0
  } catch (error) {
    console.error('获取订单列表失败:', error)
    ElMessage.error('获取订单列表失败')
    orders.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

// --- Event Handlers ---
const handleFilter = () => {
  pagination.currentPage = 1 // Reset to first page on filter change
  fetchOrders()
}

const handleResetFilters = () => {
  filters.status = ''
  handleFilter() // Fetch orders with reset filters
}

const handleSizeChange = (newSize: number) => {
  pagination.pageSize = newSize
  fetchOrders() // Fetch data when page size changes
}

const handleCurrentChange = (newPage: number) => {
  pagination.currentPage = newPage
  fetchOrders() // Fetch data when page number changes
}

const handleViewDetails = async (row: OrderVO) => {
  detailLoading.value = true
  detailDialogVisible.value = true
  selectedOrder.value = null
  try {
    // Assuming OrderVO has orderNo as the primary identifier accessible to cinema admins
    // If API needs numeric ID, and OrderVO *only* has orderNo, this might require a change
    const identifier = row.orderNo // Use orderNo instead of non-existent orderId
    if (!identifier) {
      throw new Error('订单标识符 (订单号) 无效')
    }
    const response = await getCinemaOrderDetails(identifier)
    selectedOrder.value = response.data
  } catch (error: unknown) {
    console.error('获取订单详情失败:', error)
    let message = '获取订单详情失败'
    if (isAxiosErrorWithResponse(error)) {
      message = error.response?.data?.message || message
    }
    ElMessage.error(message)
    detailDialogVisible.value = false
  } finally {
    detailLoading.value = false
  }
}

// --- Computed & Helpers ---
const getStatusTagType = (status: string | undefined): TagProps['type'] => {
  switch (status) {
    case 'PENDING_PAYMENT':
      return 'warning'
    case 'PAID':
      return 'success'
    case 'CANCELLED':
      return 'info'
    case 'REFUNDED':
      return 'danger'
    case 'COMPLETED':
      return 'primary' // Use 'primary' or 'info' instead of empty string
    default:
      return 'info'
  }
}

const getStatusText = (status: string | undefined): string => {
  const option = orderStatusOptions.find((opt) => opt.value === status)
  return option ? option.label : status || '未知'
}

// --- Lifecycle Hooks ---
onMounted(() => {
  fetchOrders()
})

// Watch for pagination changes (Alternative to @size-change/@current-change if preferred)
// watch([() => pagination.currentPage, () => pagination.pageSize], fetchOrders);
</script>

<style scoped>
.cinema-admin-orders-view {
  padding: 20px;
  height: auto;
  width: 100%;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-header h2 {
  font-size: 22px;
  font-weight: bold;
  margin: 0;
  color: #303133;
}

.filter-card {
  margin-bottom: 20px;
  overflow: visible;
}

.filter-form .el-form-item {
  margin-bottom: 10px;
  margin-right: 10px;
}

.table-card {
  margin-bottom: 20px;
  overflow: visible;
}

.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.table-title {
  font-size: 16px;
  font-weight: bold;
  color: #303133;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.dialog-footer {
  width: 100%;
  display: flex;
  justify-content: flex-end;
}

:deep(.el-card__body) {
  padding: 15px;
  overflow: visible !important;
}

:deep(.el-table) {
  width: 100% !important;
  overflow: visible;
}

:deep(.el-table__body-wrapper) {
  overflow: visible;
}

:deep(.el-descriptions__label) {
  width: 120px;
  background-color: #f5f7fa;
}
</style>
