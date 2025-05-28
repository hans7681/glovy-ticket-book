<template>
  <div class="payment-result-container">
    <el-card class="result-card">
      <div class="result-icon fail">
        <el-icon :size="64"><CircleCloseFilled /></el-icon>
      </div>

      <h1 class="result-title">支付未完成</h1>
      <p class="result-description">
        您的支付可能未成功或正在处理中，请稍后在订单列表中查看最终状态
      </p>

      <div v-if="isLoading" class="loading-state">
        <el-skeleton :rows="3" animated />
      </div>

      <div v-else-if="error" class="error-state">
        <el-alert :title="error" type="error" :closable="false" show-icon />
      </div>

      <div v-else-if="order" class="order-info">
        <el-descriptions title="订单信息" :column="1" border>
          <el-descriptions-item label="订单号">{{ order.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="电影">{{ order.movieTitle }}</el-descriptions-item>
          <el-descriptions-item label="影院">{{ order.cinemaName }}</el-descriptions-item>
          <el-descriptions-item label="场次时间">{{
            formatDateTime(order.screeningStartTime)
          }}</el-descriptions-item>
          <el-descriptions-item label="座位">
            <template v-if="order.seatsDescription && order.seatsDescription.length">
              {{ order.seatsDescription.join(', ') }}
            </template>
            <template v-else> {{ order.seatCount }} 个座位 </template>
          </el-descriptions-item>
          <el-descriptions-item label="金额"
            >￥{{ order.totalAmount.toFixed(2) }}</el-descriptions-item
          >
          <el-descriptions-item label="订单状态">{{
            getStatusText(order.status)
          }}</el-descriptions-item>
        </el-descriptions>
      </div>

      <div class="action-buttons">
        <el-button type="primary" @click="retryPayment">重新支付</el-button>
        <el-button @click="goToOrderDetail">查看订单详情</el-button>
        <el-button @click="goToHome">返回首页</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  ElIcon,
  ElCard,
  ElButton,
  ElDescriptions,
  ElDescriptionsItem,
  ElAlert,
  ElSkeleton,
} from 'element-plus'
import { CircleCloseFilled } from '@element-plus/icons-vue'
import { getOrderDetails } from '@/services/api'
import type { OrderVO } from '@/types/order'
import { formatDateTime } from '@/utils/datetime'
import axios from 'axios'

const route = useRoute()
const router = useRouter()
const orderNo = ref<string | null>((route.query.orderNo as string) || null)
const order = ref<OrderVO | null>(null)
const isLoading = ref<boolean>(!!orderNo.value)
const error = ref<string | null>(null)

// 获取订单状态的显示文本
const getStatusText = (status: string): string => {
  const statusMap: Record<string, string> = {
    PENDING_PAYMENT: '待支付',
    PAID: '已支付',
    CANCELLED: '已取消',
    REFUNDED: '已退款',
    COMPLETED: '已完成',
  }
  return statusMap[status] || status
}

// 获取订单详情
const fetchOrderDetails = async () => {
  if (!orderNo.value) {
    error.value = '未找到订单号，无法获取订单详情'
    isLoading.value = false
    return
  }

  try {
    isLoading.value = true
    const response = await getOrderDetails(orderNo.value)
    order.value = response.data as unknown as OrderVO
  } catch (err) {
    console.error('获取订单详情失败:', err)
    if (axios.isAxiosError(err) && err.response) {
      if (err.response.status === 404) {
        error.value = '找不到指定的订单'
      } else if (err.response.status === 401 || err.response.status === 403) {
        error.value = '您无权查看此订单'
        // 可能需要跳转到登录页
        // router.push('/login')
      } else {
        error.value = `获取订单详情失败 (${err.response.status})`
      }
    } else {
      error.value = '获取订单详情失败，请检查网络连接'
    }
  } finally {
    isLoading.value = false
  }
}

// 重新尝试支付
const retryPayment = () => {
  if (orderNo.value) {
    router.push({ name: 'OrderDetail', params: { identifier: orderNo.value } })
  } else {
    router.push({ name: 'OrdersList' })
  }
}

// 跳转到订单详情页
const goToOrderDetail = () => {
  if (orderNo.value) {
    router.push({ name: 'OrderDetail', params: { identifier: orderNo.value } })
  } else {
    router.push({ name: 'OrdersList' })
  }
}

// 返回首页
const goToHome = () => {
  router.push({ name: 'home' })
}

onMounted(() => {
  if (orderNo.value) {
    fetchOrderDetails()
  } else {
    isLoading.value = false
  }
})
</script>

<style scoped>
.payment-result-container {
  max-width: 800px;
  margin: 40px auto;
  padding: 0 20px;
}

.result-card {
  padding: 20px;
  text-align: center;
}

.result-icon {
  margin: 20px 0;
}

.result-icon.fail {
  color: #f56c6c;
}

.result-title {
  font-size: 24px;
  margin-bottom: 10px;
  color: #f56c6c;
}

.result-description {
  color: #666;
  margin-bottom: 30px;
}

.order-info {
  margin: 20px 0;
  text-align: left;
}

.action-buttons {
  margin-top: 30px;
  display: flex;
  justify-content: center;
  gap: 20px;
}

.loading-state,
.error-state {
  margin: 30px 0;
}
</style>
