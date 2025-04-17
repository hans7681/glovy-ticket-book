import { ref, onUnmounted, computed, watch } from 'vue'

/**
 * 创建一个响应式的倒计时器。
 * @param createTimeIsoString 订单创建时间的 ISO 字符串。
 * @param durationMinutes 倒计时的总分钟数。
 * @returns 包含剩余时间、分钟、秒和是否过期的响应式对象。
 */
export function useCountdown(
  createTimeIsoString: string | undefined | null,
  durationMinutes: number = 15,
) {
  const minutesLeft = ref(0)
  const secondsLeft = ref(0)
  const isExpired = ref(false)
  let intervalId: number | null = null

  const calculateRemainingTime = () => {
    if (!createTimeIsoString) {
      isExpired.value = true
      minutesLeft.value = 0
      secondsLeft.value = 0
      return
    }

    try {
      const createTime = new Date(createTimeIsoString)
      const deadline = new Date(createTime.getTime() + durationMinutes * 60 * 1000)
      const now = new Date()

      const remainingMilliseconds = deadline.getTime() - now.getTime()

      if (remainingMilliseconds <= 0) {
        isExpired.value = true
        minutesLeft.value = 0
        secondsLeft.value = 0
        if (intervalId) {
          clearInterval(intervalId)
          intervalId = null
        }
      } else {
        isExpired.value = false
        const totalSeconds = Math.floor(remainingMilliseconds / 1000)
        minutesLeft.value = Math.floor(totalSeconds / 60)
        secondsLeft.value = totalSeconds % 60
      }
    } catch (e) {
      console.error('Error calculating countdown:', e)
      isExpired.value = true // Mark as expired on error
      minutesLeft.value = 0
      secondsLeft.value = 0
      if (intervalId) {
        clearInterval(intervalId)
        intervalId = null
      }
    }
  }

  const startCountdown = () => {
    stopCountdown() // Clear any existing interval
    calculateRemainingTime() // Initial calculation
    if (!isExpired.value) {
      intervalId = window.setInterval(() => {
        calculateRemainingTime()
      }, 1000)
    }
  }

  const stopCountdown = () => {
    if (intervalId) {
      clearInterval(intervalId)
      intervalId = null
    }
  }

  // Watch the input time string to automatically restart the countdown if it changes
  watch(
    () => createTimeIsoString,
    () => {
      startCountdown()
    },
    { immediate: true },
  )

  // Cleanup on component unmount
  onUnmounted(() => {
    stopCountdown()
  })

  // Formatted remaining time MM:SS
  const remainingTimeFormatted = computed(() => {
    if (isExpired.value) {
      return '00:00'
    }
    const min = String(minutesLeft.value).padStart(2, '0')
    const sec = String(secondsLeft.value).padStart(2, '0')
    return `${min}:${sec}`
  })

  return {
    minutesLeft,
    secondsLeft,
    isExpired,
    remainingTimeFormatted,
    stopCountdown, // Expose stop if needed externally
    startCountdown, // Expose start if needed externally
  }
}
