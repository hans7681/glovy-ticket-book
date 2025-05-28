<template>
  <div>
    <!-- 如果状态明确是success，直接显示成功页 -->
    <PaymentSuccess v-if="status === 'success'" :orderNo="orderNo" />
    <!-- 否则，根据轮询结果决定显示内容 -->
    <div v-else>
      <div v-if="isPolling || finalStatus === 'polling'" class="polling-container">
        <el-card class="result-card">
          <div class="result-icon processing">
            <el-icon :size="64" class="is-loading"><Loading /></el-icon>
          </div>
          <h1 class="result-title processing">正在确认支付结果...</h1>
          <p class="result-description">请稍候，系统正在与支付方确认您的订单状态。</p>
          <div v-if="pollingMessage" class="polling-message">
            <el-alert :title="pollingMessage" type="info" show-icon :closable="false"></el-alert>
          </div>
          <div class="action-buttons">
            <el-button @click="goToOrderDetail" :disabled="!orderNo">查看订单详情</el-button>
            <el-button @click="goToHome">返回首页</el-button>
          </div>
        </el-card>
      </div>
      <PaymentFail v-else-if="finalStatus === 'failed'" :orderNo="orderNo" />
      <!-- 可以加一个未知状态的显示 -->
      <div v-else class="polling-container">
        <el-card class="result-card">
          <div class="result-icon fail">
            <el-icon :size="64"><WarningFilled /></el-icon>
          </div>
          <h1 class="result-title fail">支付状态未知</h1>
          <p class="result-description">无法确认支付状态，请稍后重试或联系客服。</p>
          <div class="action-buttons">
            <el-button @click="goToOrderDetail" :disabled="!orderNo">查看订单详情</el-button>
            <el-button @click="goToHome">返回首页</el-button>
          </div>
        </el-card>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElIcon, ElCard, ElButton, ElAlert } from 'element-plus'
import { Loading, WarningFilled } from '@element-plus/icons-vue'
import PaymentSuccess from './PaymentSuccess.vue'
import PaymentFail from './PaymentFail.vue'
import { queryAlipayPaymentStatus } from '@/services/api'

const props = defineProps<{
  status?: string // 从路由获取的初始状态 (success/failed/pending...)
  orderNo?: string
}>()

const router = useRouter()

const isPolling = ref(false)
const pollingMessage = ref<string>('')
const finalStatus = ref<'success' | 'failed' | 'polling' | 'unknown'>('polling') // 用于控制最终显示哪个组件
let pollingInterval: number | null = null
let pollingAttempts = 0
const maxPollingAttempts = 12 // 最多轮询12次 (大约1分钟)
const pollingIntervalTime = 5000 // 5秒轮询一次

// 停止轮询
const stopPolling = () => {
  if (pollingInterval !== null) {
    clearInterval(pollingInterval)
    pollingInterval = null
  }
  isPolling.value = false
}

// 开始轮询
const startPolling = () => {
  if (!props.orderNo) {
    console.error('No orderNo provided for polling.')
    finalStatus.value = 'failed' // 或 'unknown'
    pollingMessage.value = '缺少订单号，无法查询状态。'
    return
  }

  stopPolling() //确保之前的轮询已停止
  isPolling.value = true
  pollingAttempts = 0
  finalStatus.value = 'polling'
  pollingMessage.value = `第 ${pollingAttempts + 1} 次查询...`

  const poll = async () => {
    if (!props.orderNo) {
      stopPolling()
      finalStatus.value = 'failed' // 或 'unknown'
      pollingMessage.value = '缺少订单号，无法查询状态。'
      return
    }

    pollingAttempts++
    pollingMessage.value = `第 ${pollingAttempts} 次查询...`

    try {
      const response = await queryAlipayPaymentStatus(props.orderNo)
      if (response.data.isPaid) {
        stopPolling()
        finalStatus.value = 'success'
        // 使用 replace 跳转，避免用户回退到轮询页面
        router.replace({ path: '/payment/success', query: { orderNo: props.orderNo } })
      } else if (pollingAttempts >= maxPollingAttempts) {
        stopPolling()
        finalStatus.value = 'failed' // 轮询超时，视为失败
        pollingMessage.value = '支付结果确认超时，请稍后在订单列表查看。'
        console.warn('Polling timed out for order:', props.orderNo)
      } else {
        pollingMessage.value = `第 ${pollingAttempts} 次查询，状态：${response.data.tradeStatus || '未知'}，等待 ${pollingIntervalTime / 1000} 秒后再次查询...`
      }
    } catch (error) {
      console.error('Polling error:', error)
      pollingMessage.value = `查询出错 (${pollingAttempts}/${maxPollingAttempts})，稍后重试...`
      if (pollingAttempts >= maxPollingAttempts) {
        stopPolling()
        finalStatus.value = 'failed' // 查询多次出错，视为失败
        pollingMessage.value = '支付状态查询失败，请联系客服。'
      }
    }
  }

  // 立即执行第一次轮询
  poll()
  // 设置定时器
  pollingInterval = window.setInterval(poll, pollingIntervalTime)
}

// 返回首页
const goToHome = () => {
  router.push({ name: 'home' })
}

// 跳转到订单详情页
const goToOrderDetail = () => {
  if (props.orderNo) {
    router.push({ name: 'OrderDetail', params: { identifier: props.orderNo } })
  } else {
    // 如果没有orderNo，跳转到订单列表
    router.push({ name: 'OrdersList' })
  }
}

onMounted(() => {
  // 只有当初始状态不是success时才开始轮询
  if (props.status !== 'success') {
    startPolling()
  } else {
    finalStatus.value = 'success'
  }
})

// 组件卸载时确保停止轮询
onUnmounted(() => {
  stopPolling()
})

// 监听路由变化，如果orderNo变了重新处理 (虽然此页面一般不会这样用)
watch(
  () => props.orderNo,
  (newOrderNo, oldOrderNo) => {
    if (newOrderNo !== oldOrderNo) {
      stopPolling()
      if (props.status !== 'success') {
        startPolling()
      } else {
        finalStatus.value = 'success'
      }
    }
  },
)
</script>

<style scoped>
.polling-container {
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

.result-icon.processing {
  color: #409eff; /* Element Plus primary color */
}

.result-icon.fail {
  color: #f56c6c; /* Element Plus danger color */
}

.result-title {
  font-size: 24px;
  margin-bottom: 10px;
}
.result-title.processing {
  color: #409eff;
}
.result-title.fail {
  color: #f56c6c;
}

.result-description {
  color: #666;
  margin-bottom: 30px;
}

.polling-message {
  margin: 20px 0;
  text-align: left;
}

.action-buttons {
  margin-top: 30px;
  display: flex;
  justify-content: center;
  gap: 20px;
}
</style>
