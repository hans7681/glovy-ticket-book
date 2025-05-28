<template>
  <div class="payment-result-container">
    <el-card class="result-card">
      <div class="result-icon success">
        <el-icon :size="64"><CircleCheckFilled /></el-icon>
      </div>

      <h1 class="result-title">支付成功</h1>

      <div v-if="isLoading" class="loading-state">
        <el-skeleton :rows="5" animated />
      </div>

      <div v-else-if="error" class="error-state">
        <el-alert :title="error" type="error" :closable="false" show-icon />
      </div>

      <div v-else-if="order" class="order-info">
        <el-descriptions title="订单信息" :column="1" border>
          <el-descriptions-item label="订单号">{{ order.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="电影">{{
            screeningDetails?.movieTitle ?? order.movieTitle ?? '加载中...'
          }}</el-descriptions-item>
          <el-descriptions-item label="影院"
            >{{ screeningDetails?.cinemaName ?? order.cinemaName ?? '加载中...' }} ({{
              screeningDetails?.roomName ?? order.roomName ?? 'N/A'
            }})</el-descriptions-item
          >
          <el-descriptions-item label="场次时间">{{
            screeningDetails?.startTime
              ? formatDateTime(screeningDetails.startTime)
              : order.screeningStartTime
                ? formatDateTime(order.screeningStartTime)
                : '加载中...'
          }}</el-descriptions-item>
          <el-descriptions-item label="座位">
            <template v-if="order.seatsDescription && order.seatsDescription.length">
              {{ order.seatsDescription.join(', ') }}
            </template>
            <template v-else-if="order.seatCount"> {{ order.seatCount }} 个座位 </template>
            <template v-else> N/A </template>
          </el-descriptions-item>
          <el-descriptions-item label="金额"
            >￥{{ order.totalAmount?.toFixed(2) ?? '?.??' }}</el-descriptions-item
          >
          <el-descriptions-item label="支付时间">{{
            order.paymentTime ? formatDateTime(order.paymentTime) : 'N/A'
          }}</el-descriptions-item>
          <el-descriptions-item v-if="order.alipayTradeNo" label="支付宝交易号">
            {{ order.alipayTradeNo }}
          </el-descriptions-item>
        </el-descriptions>
        <p v-if="screeningError" class="screening-error-message">
          场次详情加载失败: {{ screeningError }}
        </p>
      </div>

      <div class="action-buttons">
        <el-button type="primary" @click="goToOrderDetail">查看订单详情</el-button>
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
import { CircleCheckFilled } from '@element-plus/icons-vue'
import { getOrderDetails, getScreeningDetails } from '@/services/api'
import type { OrderVO } from '@/types/order'
import type { ScreeningDetailVO } from '@/types/screening'
import { formatDateTime } from '@/utils/datetime'
import axios from 'axios'

const route = useRoute()
const router = useRouter()
const orderNo = ref<string | null>((route.query.orderNo as string) || null)
const order = ref<OrderVO | null>(null)
const screeningDetails = ref<ScreeningDetailVO | null>(null)
const isLoading = ref<boolean>(!!orderNo.value)
const error = ref<string | null>(null)
const screeningError = ref<string | null>(null)

const fetchDetails = async () => {
  if (!orderNo.value) {
    error.value = '未找到订单号，无法获取订单详情'
    isLoading.value = false
    return
  }

  isLoading.value = true
  error.value = null
  screeningError.value = null

  try {
    const orderResponse = await getOrderDetails(orderNo.value)
    order.value = orderResponse.data as unknown as OrderVO

    if (order.value?.status !== 'PAID') {
      console.warn('订单状态不是已支付，同步回调可能未完成或失败。当前状态:', order.value?.status)
    }

    if (
      order.value &&
      order.value.screeningId &&
      (!order.value.movieTitle || !order.value.cinemaName || !order.value.screeningStartTime)
    ) {
      try {
        const screeningData = await getScreeningDetails(order.value.screeningId)
        screeningDetails.value = screeningData
      } catch (screenErr) {
        console.error('获取场次详情失败:', screenErr)
        screeningError.value = '场次详细信息加载失败，部分信息可能无法显示。'
      }
    } else if (order.value && !order.value.screeningId) {
      console.warn('订单数据中缺少 screeningId，无法获取完整的场次详情。')
      screeningError.value = '订单关联信息不完整，无法加载场次详情。'
    }
  } catch (err) {
    console.error('获取订单详情失败:', err)
    if (axios.isAxiosError(err) && err.response) {
      if (err.response.status === 404) {
        error.value = '找不到指定的订单'
      } else if (err.response.status === 401 || err.response.status === 403) {
        error.value = '您无权查看此订单'
      } else {
        error.value = `获取订单详情失败 (${err.response.status})`
      }
    } else {
      error.value = '获取订单详情失败，请检查网络连接'
    }
    order.value = null
    screeningDetails.value = null
  } finally {
    isLoading.value = false
  }
}

const goToOrderDetail = () => {
  if (orderNo.value) {
    router.push({ name: 'OrderDetail', params: { identifier: orderNo.value } })
  } else {
    router.push({ name: 'OrdersList' })
  }
}

const goToHome = () => {
  router.push({ name: 'home' })
}

onMounted(() => {
  if (orderNo.value) {
    fetchDetails()
  } else {
    error.value = '缺少订单号'
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

.result-icon.success {
  color: #67c23a;
}

.result-title {
  font-size: 24px;
  margin-bottom: 30px;
  color: #67c23a;
}

.order-info {
  margin: 20px 0;
  text-align: left;
}

.screening-error-message {
  color: #f56c6c;
  font-size: 0.9em;
  margin-top: 10px;
  text-align: center;
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
