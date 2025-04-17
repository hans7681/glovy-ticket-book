<template>
  <div class="order-detail-container" v-loading="isLoading">
    <!-- 错误提示 -->
    <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon class="mb-4" />

    <!-- 订单详情 -->
    <template v-if="order && !error">
      <el-card class="order-card">
        <template #header>
          <div class="card-header">
            <h2 class="order-title">订单详情</h2>
            <el-tag :type="getStatusType(order.status)" class="status-tag">{{
              getStatusText(order.status)
            }}</el-tag>
          </div>
        </template>

        <div class="order-info">
          <!-- 订单基本信息 -->
          <div class="order-basic-info">
            <p class="info-item"><span class="label">订单号：</span>{{ order.orderNo }}</p>
            <p class="info-item">
              <span class="label">创建时间：</span>{{ formatDateTime(order.createTime) }}
            </p>
            <!-- 支付倒计时 -->
            <p
              v-if="order.status === 'PENDING_PAYMENT' && !isExpired && formattedTime !== '00:00'"
              class="info-item payment-countdown"
            >
              <span class="label">剩余支付时间：</span>
              <span class="countdown-timer">{{ formattedTime }}</span>
            </p>
            <p
              v-if="order.status === 'PENDING_PAYMENT' && isExpired"
              class="info-item expired-message"
            >
              <span class="label" style="color: red">支付已超时，订单已自动取消。</span>
            </p>
            <p class="info-item" v-if="order.paymentTime">
              <span class="label">支付时间：</span>{{ formatDateTime(order.paymentTime) }}
            </p>
            <p class="info-item" v-if="order.cancelTime">
              <span class="label">取消时间：</span>{{ formatDateTime(order.cancelTime) }}
            </p>
          </div>

          <!-- 影院和电影信息 -->
          <el-divider content-position="left">影片信息</el-divider>
          <div class="movie-info" v-if="screeningDetails">
            <h3 class="movie-title">{{ screeningDetails.movieTitle }}</h3>
            <p class="info-item">
              <span class="label">影院：</span>{{ screeningDetails.cinemaName }}
            </p>
            <p class="info-item">
              <span class="label">影厅：</span>{{ screeningDetails.roomName }}
            </p>
            <p class="info-item">
              <span class="label">场次时间：</span>
              {{ formatDateTime(screeningDetails.startTime) }}
            </p>
          </div>
          <div v-else class="info-item"><span class="label">场次信息：</span> 正在加载...</div>

          <!-- 座位信息 -->
          <el-divider content-position="left">座位信息</el-divider>
          <div class="seats-info">
            <div class="seat-list">
              <template v-if="hasSeatsDescription(order)">
                <el-tag
                  v-for="(seat, index) in order.seatsDescription"
                  :key="index"
                  class="seat-tag"
                >
                  {{ seat }}
                </el-tag>
              </template>
              <template v-else-if="order && Array.isArray(order.seats) && order.seats.length > 0">
                <el-tag
                  v-for="seat in order.seats"
                  :key="`${seat.rowIndex}-${seat.colIndex}`"
                  class="seat-tag"
                >
                  {{ seat.seatLabel || `${seat.rowIndex}排${seat.colIndex}座` }}
                </el-tag>
              </template>
              <template v-else>
                <span class="no-seats">没有座位信息</span>
              </template>
            </div>
            <p class="info-item"><span class="label">座位数：</span>{{ order.seatCount }} 个</p>
          </div>

          <!-- 价格信息 -->
          <el-divider content-position="left">价格信息</el-divider>
          <div class="price-info">
            <p class="total-price">
              <span class="label">总价：</span>￥{{ order.totalAmount.toFixed(2) }}
            </p>
          </div>

          <!-- 操作按钮 -->
          <div class="order-actions">
            <!-- 去支付按钮 (仅在待支付且未超时时显示) -->
            <el-button
              v-if="order?.status === 'PENDING_PAYMENT' && !isExpired"
              type="primary"
              @click="handlePayment"
              :loading="isPaying"
            >
              {{ isPaying ? '处理中...' : '去支付' }}
            </el-button>
            <!-- 取消订单按钮 (仅在待支付且未超时时显示) -->
            <el-button
              v-if="order?.status === 'PENDING_PAYMENT' && !isExpired"
              type="danger"
              plain
              @click="confirmCancelOrder"
              :loading="isCancelling"
            >
              {{ isCancelling ? '取消中...' : '取消订单' }}
            </el-button>
            <!-- 查看电影票按钮 (仅在已支付时显示) -->
            <el-button v-if="order?.status === 'PAID'" type="success" @click="showTicket">
              <el-icon class="mr-1"><Ticket /></el-icon>
              查看电影票
            </el-button>
            <!-- 可以添加其他状态下的按钮，例如"申请退款"等 -->
          </div>
        </div>
      </el-card>
    </template>

    <!-- 电影票二维码对话框 -->
    <el-dialog v-model="showTicketDialog" title="电影票二维码" width="350px" center>
      <div style="text-align: center">
        <qrcode-vue :value="ticketQrCodeValue" :size="220" level="H" render-as="svg" />
        <p style="margin-top: 15px; font-size: 0.9rem; color: #666">请在入场时出示此二维码</p>
        <!-- 显示订单关键信息 -->
        <div v-if="order" class="ticket-details">
          <p><strong>电影:</strong> {{ screeningDetails?.movieTitle || 'N/A' }}</p>
          <p>
            <strong>影院:</strong> {{ screeningDetails?.cinemaName || 'N/A' }} ({{
              screeningDetails?.roomName || 'N/A'
            }})
          </p>
          <p>
            <strong>时间:</strong>
            {{ screeningDetails ? formatDateTime(screeningDetails.startTime) : 'N/A' }}
          </p>
          <p>
            <strong>座位:</strong>
            <!-- @ts-expect-error - Linter struggles with type guard narrowing in template -->
            <template v-if="order && hasSeats(order) && order.seats && order.seats.length > 0">
              <!-- @ts-expect-error - Linter struggles with type guard narrowing in template -->
              {{
                order.seats!.map((s) => s.seatLabel || `${s.rowIndex}排${s.colIndex}座`).join(', ')
              }}
            </template>
            <template v-else-if="order && hasSeatsDescription(order)">
              {{ order.seatsDescription!.join(', ') }}
            </template>
            <template v-else> N/A </template>
          </p>
          <p><strong>订单号:</strong> {{ order.orderNo }}</p>
        </div>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button type="primary" @click="showTicketDialog = false">关闭</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import { getOrderDetails, cancelOrder, getScreeningDetails, markOrderAsPaid } from '@/services/api'
import { ElMessage, ElMessageBox, ElDialog, ElButton, ElIcon } from 'element-plus'
import { Ticket } from '@element-plus/icons-vue'
import QrcodeVue from 'qrcode.vue'
import type { Order, OrderVO, OrderSeat } from '@/types/order'
import type { ScreeningDetailVO } from '@/types/screening'
import { formatDateTime } from '@/utils/datetime'
import axios from 'axios'
import { useCountdown } from '@/composables/useCountdown'

// 使用类型守卫函数检查对象属性
function hasSeatsDescription(order: Order | OrderVO | null): order is OrderVO {
  return !!(order && 'seatsDescription' in order && Array.isArray(order.seatsDescription))
}

function hasSeats(order: Order | OrderVO | null): order is Order {
  return !!(order && 'seats' in order && Array.isArray(order.seats))
}

// 获取路由参数和初始化状态
const route = useRoute()
const orderIdentifier = route.params.identifier as string
const order = ref<Order | null>(null)
const screeningDetails = ref<ScreeningDetailVO | null>(null)
const isLoading = ref(true)
const error = ref<string | null>(null)
const isCancelling = ref(false)
const isPaying = ref(false)

// Countdown timer state
const formattedTime = ref('00:00')
const isExpired = ref(false)
const countdownStopFn = ref<(() => void) | null>(null)

// --- Ticket Dialog State ---
const showTicketDialog = ref(false)
const ticketQrCodeValue = ref('')
// --- End Ticket Dialog State ---

// 订单状态文本映射
const getStatusText = (status: string): string => {
  const statusMap: Record<string, string> = {
    PENDING_PAYMENT: '待支付',
    PAID: '已支付',
    CONFIRMED: '已确认',
    CANCELLED: '已取消',
    REFUNDED: '已退款',
    COMPLETED: '已完成',
  }
  return statusMap[status] || status
}

// 订单状态类型映射 (用于 el-tag 的类型)
const getStatusType = (status: string): 'warning' | 'success' | 'info' | 'danger' | '' => {
  const typeMap: Record<string, 'warning' | 'success' | 'info' | 'danger'> = {
    PENDING_PAYMENT: 'warning',
    PAID: 'success',
    CONFIRMED: 'success',
    CANCELLED: 'danger',
    REFUNDED: 'info',
    COMPLETED: 'success',
  }
  return typeMap[status] || ''
}

// 加载订单详情
const fetchOrderDetails = async () => {
  isLoading.value = true
  error.value = null
  order.value = null
  screeningDetails.value = null

  // Stop and clear previous countdown state
  countdownStopFn.value?.()
  countdownStopFn.value = null
  formattedTime.value = '00:00'
  isExpired.value = false

  try {
    const response = await getOrderDetails(orderIdentifier)
    console.log('获取到订单详情:', response.data)
    order.value = response.data as Order

    // Setup countdown if order is pending payment
    if (order.value && order.value.status === 'PENDING_PAYMENT') {
      const {
        remainingTimeFormatted,
        isExpired: countdownExpiredRef,
        startCountdown,
        stopCountdown,
      } = useCountdown(order.value.createTime, 15)

      formattedTime.value = remainingTimeFormatted.value
      isExpired.value = countdownExpiredRef.value

      watch(remainingTimeFormatted, (newTime) => {
        formattedTime.value = newTime
      })
      watch(countdownExpiredRef, (newExpired) => {
        isExpired.value = newExpired
        if (newExpired) {
          ElMessage.warning('支付时间已过，订单可能已被取消。正在刷新订单状态...')
          setTimeout(() => fetchOrderDetails(), 500)
        }
      })

      countdownStopFn.value = stopCountdown

      startCountdown()
    }

    // After fetching order details, fetch screening details
    if (order.value && order.value.screeningId) {
      try {
        const screeningData = await getScreeningDetails(order.value.screeningId)
        console.log('获取到场次详情:', screeningData)
        screeningDetails.value = screeningData
      } catch (screeningError) {
        console.error('获取场次详情失败:', screeningError)
        error.value = '订单信息已加载，但无法加载场次信息。'
      }
    } else if (order.value) {
      console.warn('订单数据中缺少 screeningId 或 screeningId 无效，无法获取场次详情。')
      error.value = '订单信息已加载，但场次关联信息缺失。'
    } else {
      console.error('获取订单详情后，order.value 仍然为 null')
      error.value = '无法加载订单数据。'
    }
  } catch (err: unknown) {
    console.error('获取订单详情失败:', err)
    if (axios.isAxiosError(err) && err.response) {
      if (err.response.status === 404) {
        error.value = '找不到指定的订单。'
      } else if (err.response.status === 401 || err.response.status === 403) {
        error.value = '您无权查看此订单。'
      } else {
        error.value = `加载订单详情时出错 (${err.response.status})。请检查网络连接或稍后再试。`
      }
    } else {
      error.value = '加载订单详情时发生未知错误。请检查网络连接或稍后再试。'
    }
    order.value = null
    screeningDetails.value = null
  } finally {
    isLoading.value = false
  }
}

// === 模拟支付处理 ===
const handlePayment = async () => {
  if (!order.value || isExpired.value) return
  isPaying.value = true
  try {
    // TODO: Implement actual payment logic here
    // Example: Simulate payment success after 2 seconds
    await new Promise((resolve) => setTimeout(resolve, 2000))
    // Simulate marking order as paid (replace with actual API call if needed)
    // Or better: Redirect to a payment gateway, and have a webhook update the order status
    // For now, just simulate and refresh
    ElMessage.success('支付成功！正在更新订单状态...')
    await markOrderAsPaid(order.value.orderNo) // Assuming an API exists to mark as paid locally for demo
    await fetchOrderDetails() // Refresh details
  } catch (err) {
    console.error('支付处理失败:', err)
    ElMessage.error('支付失败，请稍后再试。')
  } finally {
    isPaying.value = false
  }
}
// === 结束模拟支付处理 ===

// 取消订单
const confirmCancelOrder = () => {
  if (!order.value || isExpired.value) return

  ElMessageBox.confirm('确定要取消此订单吗？取消后不可恢复。', '取消订单', {
    confirmButtonText: '确认取消',
    cancelButtonText: '我再想想',
    type: 'warning',
  })
    .then(async () => {
      try {
        isCancelling.value = true
        await cancelOrder(orderIdentifier)

        ElMessage({
          type: 'success',
          message: '订单已成功取消',
        })

        await fetchOrderDetails()
      } catch (err: unknown) {
        console.error('取消订单失败:', err)

        let errorMsg = '取消订单失败，请稍后重试。'
        if (axios.isAxiosError(err)) {
          const status = err.response?.status
          const responseData = err.response?.data as { message?: string }

          if (status === 400) {
            errorMsg = responseData?.message || '无法取消订单，订单状态可能已不允许取消。'
          } else if (status === 403) {
            errorMsg = '您无权取消此订单。'
          } else if (status === 404) {
            errorMsg = '订单不存在或已被删除。'
          }
        }
        ElMessage.error(errorMsg)
        await fetchOrderDetails()
      } finally {
        isCancelling.value = false
      }
    })
    .catch(() => {
      ElMessage.info('取消操作已取消')
    })
}

// --- Show Ticket Logic ---
const showTicket = () => {
  if (order.value && screeningDetails.value) {
    const ticketData = {
      orderNo: order.value.orderNo,
      movie: screeningDetails.value.movieTitle,
      cinema: screeningDetails.value.cinemaName,
      room: screeningDetails.value.roomName,
      time: screeningDetails.value.startTime,
      seats: [] as string[],
      ticketId: `TICKET-${order.value.orderNo}`,
    }

    // Populate seats based on available data with refined checks and non-null assertions
    // @ts-expect-error - Linter struggles with type guard narrowing here
    if (hasSeats(order.value) && order.value.seats.length > 0) {
      ticketData.seats = order.value.seats!.map(
        (s: OrderSeat) => s.seatLabel || `${s.rowIndex}排${s.colIndex}座`,
      )
    } else if (hasSeatsDescription(order.value) && order.value.seatsDescription) {
      ticketData.seats = order.value.seatsDescription!
    }

    ticketQrCodeValue.value = JSON.stringify(ticketData, null, 2)
    console.log('Ticket QR Code Data:', ticketQrCodeValue.value)
    showTicketDialog.value = true
  } else {
    ElMessage.warning('无法生成电影票，订单或场次信息不完整。')
    console.error('Attempted to show ticket but order or screeningDetails is null:', {
      order: order.value,
      screening: screeningDetails.value,
    })
  }
}
// --- End Show Ticket Logic ---

// 组件挂载时获取订单详情
onMounted(() => {
  fetchOrderDetails()
})

// Cleanup countdown timer on component unmount
onUnmounted(() => {
  countdownStopFn.value?.()
})
</script>

<style scoped>
.order-detail-container {
  max-width: 800px;
  margin: 20px auto;
  padding: 0 15px;
}

.mb-4 {
  margin-bottom: 20px;
}

.order-card {
  margin-bottom: 20px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.order-title {
  margin: 0;
  font-size: 1.25rem;
  font-weight: bold;
}

.status-tag {
  font-weight: bold;
  font-size: 0.9rem;
}

.order-info {
  padding: 10px 0;
}

.info-item {
  margin: 12px 0;
  line-height: 1.6;
  font-size: 0.95rem;
}

.label {
  font-weight: bold;
  color: #555;
  margin-right: 8px;
  display: inline-block;
  min-width: 70px;
}

.movie-title {
  font-size: 1.1rem;
  margin: 15px 0 10px;
  color: #333;
  font-weight: 600;
}

.seats-info {
  margin: 20px 0;
}

.seat-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 12px;
  margin-bottom: 15px;
  padding: 10px;
  background-color: #f9f9f9;
  border-radius: 4px;
}

.seat-tag {
  padding: 5px 10px;
  font-size: 0.9rem;
}

.no-seats {
  color: #909399;
  font-style: italic;
}

.price-info {
  margin-top: 15px;
}

.total-price {
  font-size: 1.3rem;
  color: #e6a23c;
  font-weight: bold;
  text-align: right;
  margin-top: 5px;
}

.total-price .label {
  color: #333;
}

.order-actions {
  margin-top: 30px;
  display: flex;
  gap: 15px;
  justify-content: flex-end;
  padding-top: 20px;
  border-top: 1px solid #eee;
  flex-wrap: wrap;
}

/* Responsive adjustments */
@media (max-width: 768px) {
  .order-title {
    font-size: 1.1rem;
  }
  .card-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .status-tag {
    margin-top: 10px;
  }

  .order-actions {
    flex-direction: column;
    align-items: stretch;
    justify-content: center;
  }

  .order-actions .el-button {
    margin-bottom: 10px;
  }
  .order-actions .el-button:last-child {
    margin-bottom: 0;
  }

  .total-price {
    text-align: left;
    font-size: 1.2rem;
  }

  .seat-list {
    padding: 8px;
  }
}

.mr-1 {
  margin-right: 4px;
}

/* Center dialog content */
.el-dialog__body {
  text-align: center;
}

.ticket-details {
  font-size: 0.85rem;
  margin-top: 20px;
  text-align: left;
  display: inline-block;
  color: #333;
  line-height: 1.6;
  padding: 10px;
  background-color: #f8f9fa;
  border-radius: 4px;
  border: 1px solid #eee;
}

.ticket-details p {
  margin: 5px 0;
}

.ticket-details strong {
  color: #555;
  margin-right: 5px;
}

/* Adjust dialog width slightly if needed */
@media (max-width: 400px) {
  /* The existing width="350px" might be too large */
  /* We can use CSS vars or dynamic binding if more control is needed, */
  /* but for now, rely on Element Plus's responsiveness */
}

.payment-countdown .countdown-timer {
  font-weight: bold;
  color: #e6a23c; /* Warning color for countdown */
}

.expired-message .label {
  color: #f56c6c; /* Danger color for expired message */
  font-weight: bold;
}
</style>
