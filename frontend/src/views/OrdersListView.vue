<template>
  <div class="orders-list-view">
    <h2>我的订单</h2>

    <el-tabs v-model="filterStatus" @tab-click="handleFilterChange" class="filter-tabs">
      <el-tab-pane label="全部" name="all"></el-tab-pane>
      <el-tab-pane label="待支付" name="PENDING_PAYMENT"></el-tab-pane>
      <el-tab-pane label="已支付" name="PAID"></el-tab-pane>
      <el-tab-pane label="已取消" name="CANCELLED"></el-tab-pane>
      <!-- Add other statuses if needed -->
    </el-tabs>

    <el-card v-loading="loading" class="box-card">
      <el-alert
        v-if="error"
        :title="error"
        type="error"
        show-icon
        :closable="false"
        style="margin-bottom: 20px"
      />

      <el-table :data="orders" style="width: 100%">
        <el-table-column prop="orderNo" label="订单号" width="240" />
        <el-table-column label="电影">
          <template #default="{ row }">
            {{ row.movie?.title ?? '-' }}
          </template>
        </el-table-column>
        <el-table-column label="放映时间" width="160">
          <template #default="{ row }">
            {{ row.screening?.startTime ? formatDateTime(row.screening.startTime) : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="金额" width="90">
          <template #default="{ row }"> ￥{{ row.totalAmount?.toFixed(2) ?? '0.00' }} </template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <div class="status-wrapper">
              <el-tag :type="getStatusTagType(row.status)">{{ getStatusText(row.status) }}</el-tag>
              <span v-if="row.status === 'PENDING_PAYMENT'" class="countdown">
                <small>({{ getOrderCountdown(row.createTime) }})</small>
              </span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="230" fixed="right">
          <template #default="{ row, $index }">
            <div class="actions-wrapper">
              <el-button type="primary" size="small" @click="goToOrderDetail(row.id)"
                >详情</el-button
              >
              <el-button
                v-if="row.status === 'PENDING_PAYMENT'"
                type="warning"
                size="small"
                @click="goToOrderDetail(row.id)"
                >去支付</el-button
              >
              <el-button
                v-if="row.status === 'PENDING_PAYMENT'"
                type="danger"
                size="small"
                @click="handleCancelOrder(row.id, $index)"
                >取消订单</el-button
              >
            </div>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-if="pagination.totalItems > 0"
        background
        layout="prev, pager, next, sizes, total"
        :total="pagination.totalItems"
        :current-page="pagination.currentPage"
        :page-size="pagination.pageSize"
        :page-sizes="[5, 10, 20, 50]"
        @current-change="handlePageChange"
        @size-change="handleSizeChange"
        style="margin-top: 20px; justify-content: flex-end"
      />

      <el-empty v-if="!loading && orders.length === 0 && !error" description="暂无订单" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  ElTabs,
  ElTabPane,
  ElCard,
  ElTable,
  ElTableColumn,
  ElTag,
  ElButton,
  ElPagination,
  ElEmpty,
  ElAlert,
  ElMessageBox,
  ElMessage,
} from 'element-plus'
import { listMyOrders, cancelOrder } from '@/services/api'
import type { OrderVO } from '@/types/order'
import { formatDateTime } from '@/utils/datetime'
// import { useCountdown } from '@/composables/useCountdown'
import axios from 'axios'

type OrderStatus = 'PENDING_PAYMENT' | 'PAID' | 'CANCELLED' | 'REFUNDED' | 'COMPLETED'

const router = useRouter()

// State
const orders = ref<OrderVO[]>([])
const loading = ref(true)
const error = ref<string | null>(null)
const pagination = reactive({
  currentPage: 1,
  pageSize: 10,
  totalItems: 0,
})
const filterStatus = ref<string>('all') // 'all' or OrderStatus

// Fetch Orders Function
const fetchOrders = async () => {
  loading.value = true
  error.value = null
  try {
    const params: {
      current: number
      size: number
      status?: OrderStatus | null
    } = {
      current: pagination.currentPage,
      size: pagination.pageSize,
      status: filterStatus.value === 'all' ? null : (filterStatus.value as OrderStatus),
    }
    const response = await listMyOrders(params)
    orders.value = response.data.records || []
    pagination.totalItems = response.data.total || 0
  } catch (err) {
    console.error('Failed to fetch orders:', err)
    if (axios.isAxiosError(err) && err.response?.status === 401) {
      error.value = '请先登录以查看订单。'
      // Optionally redirect to login or show login prompt
    } else {
      error.value = '加载订单失败，请稍后重试。'
    }
    orders.value = []
    pagination.totalItems = 0
  } finally {
    loading.value = false
  }
}

// Lifecycle Hook
onMounted(() => {
  fetchOrders()
})

// Event Handlers
const handleFilterChange = () => {
  pagination.currentPage = 1 // Reset to first page when filter changes
  fetchOrders()
}

const handlePageChange = (page: number) => {
  pagination.currentPage = page
  fetchOrders()
}

const handleSizeChange = (size: number) => {
  pagination.pageSize = size
  pagination.currentPage = 1 // Reset to first page when size changes
  fetchOrders()
}

const goToOrderDetail = (orderId: number | string) => {
  router.push({ name: 'OrderDetail', params: { identifier: orderId.toString() } })
}

const handleCancelOrder = async (orderId: number | string, index: number) => {
  try {
    await ElMessageBox.confirm('确定要取消此订单吗？此操作无法撤销。', '取消订单确认', {
      confirmButtonText: '确定取消',
      cancelButtonText: '再想想',
      type: 'warning',
    })
    // User confirmed
    loading.value = true
    try {
      await cancelOrder(orderId)
      ElMessage.success('订单已取消！')
      // Update the status locally using the index
      if (index >= 0 && index < orders.value.length) {
        // Verify the id matches before updating, just in case
        if (orders.value[index].id === orderId) {
          orders.value[index].status = 'CANCELLED'
        } else {
          console.warn(
            'Index mismatch during local order cancellation update. Fetching fresh list.',
          )
          await fetchOrders() // Fallback to fetching if index seems wrong
        }
      } else {
        console.warn(
          'Invalid index provided for local order cancellation update. Fetching fresh list.',
        )
        await fetchOrders() // Fallback if index is invalid
      }
    } catch (cancelErr) {
      console.error('Failed to cancel order:', cancelErr)
      if (axios.isAxiosError(cancelErr) && cancelErr.response) {
        if (cancelErr.response.status === 400) {
          ElMessage.error(`取消失败：${cancelErr.response.data || '订单状态不允许取消。'}`)
        } else {
          ElMessage.error('取消订单失败，请稍后重试。')
        }
      } else {
        ElMessage.error('取消订单失败，请稍后重试。')
      }
    } finally {
      loading.value = false
    }
  } catch {
    // User clicked "再想想" or closed the dialog
    ElMessage.info('取消操作已取消')
  }
}

// --- Helper Functions for UI ---
const getStatusTagType = (status: OrderStatus | string) => {
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
      return 'primary' // Or 'success'
    default:
      return 'info'
  }
}

const getStatusText = (status: OrderStatus | string) => {
  switch (status) {
    case 'PENDING_PAYMENT':
      return '待支付'
    case 'PAID':
      return '已支付'
    case 'CANCELLED':
      return '已取消'
    case 'REFUNDED':
      return '已退款'
    case 'COMPLETED':
      return '已完成'
    default:
      return '未知状态'
  }
}

const getOrderCountdown = (createTime: string): string => {
  if (!createTime) return '00:00'

  const orderTime = new Date(createTime).getTime()
  const currentTime = new Date().getTime()

  // 订单有效期15分钟
  const expiryTime = orderTime + 15 * 60 * 1000
  const remainingMs = expiryTime - currentTime

  if (remainingMs <= 0) return '00:00'

  const minutes = Math.floor(remainingMs / (60 * 1000))
  const seconds = Math.floor((remainingMs % (60 * 1000)) / 1000)

  return `${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`
}

// 更新倒计时
const updateCountdowns = () => {
  // 强制Vue重新渲染，以更新时间
  orders.value = [...orders.value]
}

// 设置倒计时定时器
let countdownTimer: number | null = null

onMounted(() => {
  fetchOrders()

  // 设置倒计时刷新定时器
  countdownTimer = window.setInterval(() => {
    updateCountdowns()
  }, 1000) as unknown as number
})

// 在组件卸载时清除定时器
onUnmounted(() => {
  if (countdownTimer !== null) {
    clearInterval(countdownTimer)
  }
})
</script>

<style scoped>
.orders-list-view {
  max-width: 1200px;
  margin: 20px auto;
  padding: 20px;
}

h2 {
  margin-bottom: 20px;
  text-align: left;
  font-size: 1.5rem;
  color: #333;
}

.filter-tabs {
  margin-bottom: 20px;
}

.box-card {
  /* Style card if needed */
}

.status-wrapper {
  display: flex;
  align-items: center;
}

.countdown {
  margin-left: 5px;
  color: #e6a23c;
}

.actions-wrapper {
  display: flex;
  justify-content: flex-start;
  gap: 5px;
}

/* Add more specific styles if needed */
</style>
