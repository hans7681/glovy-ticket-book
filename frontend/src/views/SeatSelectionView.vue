<template>
  <div
    class="seat-selection-view"
    v-loading="isDetailLoading || isSeatLoading"
    element-loading-text="加载中..."
  >
    <template v-if="!error">
      <!-- 1. Screening Details Section -->
      <el-card v-if="screeningDetails" class="screening-info-card">
        <template #header>
          <div class="card-header">
            <span>场次信息</span>
          </div>
        </template>
        <div class="screening-details">
          <el-image
            v-if="screeningDetails.moviePosterUrl"
            :src="screeningDetails.moviePosterUrl"
            fit="contain"
            class="movie-poster-small"
          />
          <div class="info-text">
            <h3>{{ screeningDetails.movieTitle }}</h3>
            <p>
              <el-icon><OfficeBuilding /></el-icon> {{ screeningDetails.cinemaName }} -
              {{ screeningDetails.roomName }}
            </p>
            <p>
              <el-icon><Clock /></el-icon> {{ formatDisplayDateTime(screeningDetails.startTime) }}
            </p>
            <p>
              <el-icon><Money /></el-icon> 票价: ¥{{ screeningDetails.price?.toFixed(2) }}
            </p>
          </div>
        </div>
      </el-card>
      <el-skeleton :rows="3" animated v-else />

      <!-- 2. Seat Map Section -->
      <el-card class="seat-map-card">
        <template #header>
          <div class="card-header legend-header">
            <span>选择座位 (最多选择 {{ maxSeats }} 个)</span>
            <!-- Seat Legend -->
            <div class="seat-legend">
              <div class="legend-item"><span class="seat available"></span> 可选</div>
              <div class="legend-item"><span class="seat selected"></span> 已选</div>
              <div class="legend-item"><span class="seat sold"></span> 已售</div>
              <div class="legend-item"><span class="seat unavailable"></span> 不可用</div>
              <!-- Add LOCKED if backend distinguishes it -->
            </div>
          </div>
        </template>
        <div v-if="seatRows.length > 0" class="seat-map-container">
          <div class="screen-indicator">银幕 Screen</div>
          <div v-for="(row, rowIndex) in seatRows" :key="rowIndex" class="seat-row">
            <div class="row-indicator">{{ rowIndex + 1 }}排</div>
            <div
              v-for="(seat, colIndex) in row"
              :key="`${rowIndex}-${colIndex}`"
              class="seat"
              :class="getSeatClass(seat)"
              @click="toggleSeatSelection(seat)"
            >
              <!-- Optionally display seat number if available: {{ seat.seatLabel }} -->
            </div>
          </div>
        </div>
        <el-skeleton :rows="10" animated v-else />
      </el-card>

      <!-- 3. Selection Summary & Confirmation Section -->
      <el-card class="selection-summary-card">
        <div class="summary-content">
          <div class="selection-summary-info">
            <span>已选座位：</span>
            <template v-if="selectedSeats.length > 0">
              <el-tag
                v-for="seat in selectedSeats"
                :key="`${seat.rowIndex}-${seat.colIndex}`"
                class="selected-seat-tag"
                closable
                @close="handleDeselectFromTag(seat)"
              >
                {{ seat.rowIndex + 1 }}排{{ seat.colIndex + 1 }}座
              </el-tag>
            </template>
            <span v-else class="no-seats-text">请在上方选择座位</span>
          </div>
          <div class="price-and-confirm">
            <span class="total-price"
              >总计：<span class="price-value">¥{{ totalPrice.toFixed(2) }}</span></span
            >
            <el-button
              type="danger"
              size="large"
              :disabled="isConfirmDisabled"
              @click="confirmSelection"
              :loading="confirming"
            >
              确认选座
            </el-button>
          </div>
        </div>
      </el-card>
    </template>
    <el-alert v-else title="加载失败" :description="error" type="error" show-icon />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  getScreeningDetails,
  getScreeningSeatStatus,
  lockSeats,
  unlockSeats,
  createOrder,
} from '@/services/api'
import { useAuthStore } from '@/stores/auth'
import type { ScreeningDetailVO } from '@/types/screening'
import type { SeatStatus, SeatIdentifier } from '@/types/seat'
import type { CreateOrderRequest, SeatInfo, OrderVO } from '@/types/order'
import {
  ElMessage,
  ElButton,
  ElCard,
  ElSkeleton,
  ElAlert,
  ElTag,
  ElIcon,
  ElImage,
} from 'element-plus'
import { OfficeBuilding, Clock, Money } from '@element-plus/icons-vue'
import axios from 'axios'

const authStore = useAuthStore()
const props = defineProps<{ screeningId: number }>()
const router = useRouter()

// --- Core State ---
const screeningDetails = ref<ScreeningDetailVO | null>(null)
const seatRows = ref<SeatStatus[][]>([]) // 2D array, directly from API now
const seatLayout = ref({ rows: 0, cols: 0 })
const selectedSeats = ref<SeatIdentifier[]>([])
const isSeatLoading = ref(true)
const isDetailLoading = ref(true)
const confirming = ref(false) // Loading state for confirmation button
const error = ref<string | null>(null)
const maxSeats = ref(4) // Max selectable seats

// Keep track of seats locked by *this* user in *this* session
const userLockedSeats = ref<SeatIdentifier[]>([])

// --- Computed Properties ---
const totalPrice = computed(() => {
  return selectedSeats.value.length * (screeningDetails.value?.price || 0)
})

const isConfirmDisabled = computed(() => {
  return selectedSeats.value.length === 0 || isSeatLoading.value || isDetailLoading.value
})

// --- Helper Functions ---
const formatDisplayDateTime = (isoString: string | undefined | null): string => {
  if (!isoString) return 'N/A'
  try {
    const date = new Date(isoString)
    return date.toLocaleString('zh-CN', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    })
  } catch {
    return 'Invalid Date'
  }
}

const findSeatInGrid = (rowIndex: number, colIndex: number): SeatStatus | undefined => {
  // Directly access the 2D array
  if (seatRows.value[rowIndex] && seatRows.value[rowIndex][colIndex]) {
    return seatRows.value[rowIndex][colIndex]
  }
  return undefined
}

const isSeatSelected = (seat: SeatStatus): boolean => {
  // Compare 0-based SeatStatus with 0-based selectedSeats
  return selectedSeats.value.some(
    (s) => s.rowIndex === seat.rowIndex && s.colIndex === seat.colIndex,
  )
}

const getSeatClass = (seat: SeatStatus): string[] => {
  const classes = ['seat']
  const isSelected = isSeatSelected(seat)

  if (isSelected) {
    classes.push('selected')
  } else {
    switch (seat.status) {
      case 'AVAILABLE':
        classes.push('available')
        break
      case 'SOLD':
      case 'LOCKED': // Treat LOCKED same as SOLD visually unless specified otherwise
        classes.push('sold')
        break
      case 'UNAVAILABLE':
      default:
        classes.push('unavailable')
        break
    }
  }
  return classes
}

// --- Core Logic ---
const fetchScreeningDetails = async () => {
  try {
    screeningDetails.value = await getScreeningDetails(props.screeningId)
  } catch (err) {
    console.error('Failed to load screening details:', err)
    error.value = '加载场次详情失败，请稍后再试。'
    throw err // Re-throw to be caught by Promise.all
  }
}

const fetchSeatStatus = async () => {
  try {
    const responseGrid = await getScreeningSeatStatus(props.screeningId)
    console.log('API Response (Seats Grid Raw):', JSON.parse(JSON.stringify(responseGrid))) // Log raw response

    if (
      !Array.isArray(responseGrid) ||
      (responseGrid.length > 0 && !Array.isArray(responseGrid[0]))
    ) {
      console.error('Invalid seat grid data structure received from API:', responseGrid)
      throw new Error('从API接收到的座位网格数据结构无效')
    }

    // --- Transform API response (assuming 1-based) to 0-based for internal use ---
    const internalGrid: SeatStatus[][] = responseGrid.map((row) =>
      row.map((seat) => ({
        ...seat,
        // Convert potentially 1-based indices from API to 0-based for internal state
        rowIndex: seat.rowIndex - 1,
        colIndex: seat.colIndex - 1,
      })),
    )
    console.log('Internal Seat Grid (0-based):', JSON.parse(JSON.stringify(internalGrid))) // Log transformed grid

    // Validate transformed indices (optional but good practice)
    internalGrid.forEach((row, rIdx) => {
      row.forEach((seat, cIdx) => {
        if (seat.rowIndex !== rIdx || seat.colIndex !== cIdx) {
          console.warn(
            `Index mismatch after transform at [${rIdx},${cIdx}]: seat has [${seat.rowIndex},${seat.colIndex}]. Check API index base.`,
          )
          // Potentially correct them if needed, though the map should handle it.
          // seat.rowIndex = rIdx;
          // seat.colIndex = cIdx;
        }
      })
    })
    // --------------------------------------------------------------------------

    // Assign the transformed 0-based grid to our reactive state
    seatRows.value = internalGrid

    // Infer layout from the transformed grid dimensions
    const rows = internalGrid.length
    const cols = internalGrid[0]?.length || 0
    seatLayout.value = { rows, cols }
  } catch (err) {
    console.error('Failed to load seat status:', err)
    if (!error.value) {
      error.value = '加载座位图失败，请稍后再试。'
    }
    throw err
  }
}

const handleSelect = async (seat: SeatStatus) => {
  const seatId: SeatIdentifier = { rowIndex: seat.rowIndex, colIndex: seat.colIndex } // 0-based ID for internal state

  // 1. Frontend Pre-update (Optimistic UI)
  selectedSeats.value.push(seatId) // Store 0-based ID
  userLockedSeats.value.push(seatId) // Store 0-based ID

  const seatInGrid = findSeatInGrid(seat.rowIndex, seat.colIndex)
  if (seatInGrid) seatInGrid.status = 'SELECTED'

  // 2. Call Lock API - Needs 1-based IDs
  const apiSeatId: SeatIdentifier = { rowIndex: seat.rowIndex + 1, colIndex: seat.colIndex + 1 }
  try {
    // Pass 1-based ID to API
    await lockSeats(props.screeningId, [apiSeatId])
    // Lock successful
  } catch (lockError: unknown) {
    console.error('Failed to lock seat:', lockError)
    // 3. Rollback on failure - Use 0-based ID for internal state
    selectedSeats.value = selectedSeats.value.filter(
      (s) => !(s.rowIndex === seatId.rowIndex && s.colIndex === seatId.colIndex),
    )
    userLockedSeats.value = userLockedSeats.value.filter(
      (s) => !(s.rowIndex === seatId.rowIndex && s.colIndex === seatId.colIndex),
    )
    if (seatInGrid) seatInGrid.status = 'AVAILABLE' // Revert status

    // Type check before accessing response properties
    let isConflict = false
    if (typeof lockError === 'object' && lockError !== null && 'response' in lockError) {
      const responseError = lockError as { response?: { status?: number } }
      isConflict = responseError.response?.status === 409
    }

    if (isConflict) {
      ElMessage.error('手慢了，座位已被占用！')
      fetchSeatStatus()
    } else {
      ElMessage.error('锁定座位失败，请重试。')
    }
  }
}

const handleDeselect = async (seat: SeatStatus) => {
  // Takes SeatStatus (0-based)
  const seatId: SeatIdentifier = { rowIndex: seat.rowIndex, colIndex: seat.colIndex } // 0-based ID

  // 1. Frontend Update
  selectedSeats.value = selectedSeats.value.filter(
    (s) => !(s.rowIndex === seatId.rowIndex && s.colIndex === seatId.colIndex),
  )
  const seatInGrid = findSeatInGrid(seatId.rowIndex, seatId.colIndex)
  if (seatInGrid) seatInGrid.status = 'AVAILABLE'

  // 2. Call Unlock API - Needs 1-based IDs
  const wasLockedByUser = userLockedSeats.value.some(
    (lockedSeat) =>
      lockedSeat.rowIndex === seatId.rowIndex && lockedSeat.colIndex === seatId.colIndex,
  )

  if (wasLockedByUser) {
    userLockedSeats.value = userLockedSeats.value.filter(
      (s) => !(s.rowIndex === seatId.rowIndex && s.colIndex === seatId.colIndex),
    )
    const apiSeatId: SeatIdentifier = {
      rowIndex: seatId.rowIndex + 1,
      colIndex: seatId.colIndex + 1,
    } // Convert to 1-based
    try {
      await unlockSeats(props.screeningId, [apiSeatId])
    } catch (unlockError) {
      console.warn('Failed to unlock seat:', unlockError)
    }
  }
}

// New function to handle deselection from the tag click
const handleDeselectFromTag = (seatId: SeatIdentifier) => {
  // seatId is the 0-based identifier from selectedSeats
  const seatStatus = findSeatInGrid(seatId.rowIndex, seatId.colIndex)
  if (seatStatus) {
    handleDeselect(seatStatus) // Call the main deselect logic with the SeatStatus object
  } else {
    console.warn('Could not find SeatStatus for tag deselect:', seatId)
    // Fallback or alternative logic if needed, though this shouldn't happen
  }
}

const toggleSeatSelection = (seat: SeatStatus) => {
  const isSelected = isSeatSelected(seat)

  if (seat.status !== 'AVAILABLE' && !isSelected) {
    return // Cannot select SOLD, LOCKED (by others), or UNAVAILABLE seats
  }

  if (isSelected) {
    handleDeselect(seat)
  } else {
    if (selectedSeats.value.length >= maxSeats.value) {
      ElMessage.warning(`最多只能选择 ${maxSeats.value} 个座位。`)
      return
    }
    handleSelect(seat)
  }
}

const confirmSelection = async () => {
  if (isConfirmDisabled.value || confirming.value) return
  confirming.value = true

  console.log('--- 确认选座 开始 --- ')
  console.log('Screening ID:', props.screeningId)

  // Prepare 1-based IDs for API call
  const apiSelectedSeats: SeatInfo[] = selectedSeats.value.map((s) => ({
    rowIndex: s.rowIndex + 1, // Convert to 1-based
    colIndex: s.colIndex + 1,
  }))
  console.log('Selected Seats (1-based for API):', apiSelectedSeats)

  const orderRequestData: CreateOrderRequest = {
    screeningId: props.screeningId,
    selectedSeats: apiSelectedSeats,
  }

  try {
    console.log('Calling createOrder API...')
    const response = await createOrder(orderRequestData)
    const newOrder: OrderVO = response.data
    console.log('Order created successfully:', newOrder)

    ElMessage.success('订单创建成功！正在跳转到订单详情...')

    // Clear local selection state after successful order creation
    // Note: Also clear userLockedSeats as these are now part of the order
    const seatsJustOrdered = [...selectedSeats.value]
    selectedSeats.value = []
    userLockedSeats.value = userLockedSeats.value.filter(
      (lockedSeat) =>
        !seatsJustOrdered.some(
          (orderedSeat) =>
            orderedSeat.rowIndex === lockedSeat.rowIndex &&
            orderedSeat.colIndex === lockedSeat.colIndex,
        ),
    )

    // Redirect to order detail page (using orderNo or orderNo)
    // Adjust 'OrderDetail' and param name if your route is different
    router.push({
      name: 'OrderDetail',
      params: { identifier: newOrder.orderNo },
    })
  } catch (err: unknown) {
    console.error('Failed to create order:', err)
    let errorMessage = '创建订单失败，请稍后再试。' // Default message

    if (axios.isAxiosError(err)) {
      const errorResponse = err.response
      if (errorResponse) {
        const status = errorResponse.status
        const responseData = errorResponse.data // Assuming backend sends useful error messages
        console.error(`Error Status: ${status}`, responseData)

        switch (status) {
          case 400:
            // Check for specific backend message if available
            errorMessage = responseData?.message || '请求无效，请检查座位选择或场次信息。'
            break
          case 401:
            errorMessage = '用户未登录或登录已过期，请重新登录。'
            // Consider redirecting to login: router.push('/login')
            break
          case 404:
            errorMessage = '场次信息不存在，请返回重试。'
            break
          case 409:
            errorMessage =
              responseData?.message || '抱歉，您选择的部分或全部座位已被占用，请重新选择。'
            // Refresh seat status after conflict
            fetchSeatStatus().catch((fetchErr) =>
              console.error('Failed to refresh seats after conflict:', fetchErr),
            )
            break
          case 500:
            errorMessage = '服务器内部错误，请稍后再试。'
            break
          default:
            errorMessage = `发生未知错误 (HTTP ${status})。`
        }
      } else if (err.request) {
        // Request was made but no response received
        errorMessage = '无法连接到服务器，请检查网络连接。'
      } else {
        // Something happened in setting up the request
        errorMessage = '创建订单时发生内部错误。'
      }
    } else {
      // Not an Axios error (e.g., network error, client-side JS error)
      console.error('Non-Axios error:', err)
    }

    ElMessage.error(errorMessage)
  } finally {
    confirming.value = false // Reset loading state
    console.log('--- 确认选座 结束 --- ')
  }
}

// --- Lifecycle Hooks ---
onMounted(() => {
  isSeatLoading.value = true
  isDetailLoading.value = true
  error.value = null

  Promise.all([fetchScreeningDetails(), fetchSeatStatus()])
    .catch(() => {
      if (!error.value) {
        error.value = '加载页面数据时出错，请刷新重试。'
      }
    })
    .finally(() => {
      isSeatLoading.value = false
      isDetailLoading.value = false
    })
})

onUnmounted(() => {
  // Use fetch with keepalive: true for reliable unlocking on page unload
  const seatsToUnlock = [...userLockedSeats.value]
  console.log(
    `[onUnmounted] Hook triggered. Seats locked in this session (0-based):`,
    JSON.parse(JSON.stringify(seatsToUnlock)),
  )

  if (seatsToUnlock.length > 0) {
    console.log('[onUnmounted] Attempting to unlock seats using fetch+keepalive...')
    const apiSeatsToUnlock = seatsToUnlock.map((s) => ({
      rowIndex: s.rowIndex + 1, // Convert to 1-based for API
      colIndex: s.colIndex + 1,
    }))

    const url = `/api/screenings/${props.screeningId}/unlock-seats`
    const token = authStore.token
    const headers: HeadersInit = {
      'Content-Type': 'application/json',
    }
    if (token) {
      headers['Authorization'] = `Bearer ${token}`
    }

    const body = JSON.stringify({ seatsToUnlock: apiSeatsToUnlock })

    try {
      fetch(url, {
        method: 'DELETE',
        headers: headers,
        body: body,
        keepalive: true, // Important!
      })
      console.log('[onUnmounted] Unlock request sent via fetch with keepalive: true.')

      // Clear local state optimistically as the request is sent
      userLockedSeats.value = []
    } catch (err) {
      // Catch synchronous errors during fetch setup, though unlikely
      console.error('[onUnmounted] Synchronous error setting up fetch for unlock:', err)
    }
  } else {
    console.log('[onUnmounted] No seats were locked in this session, skipping unlock.')
  }
})
</script>

<style scoped>
.seat-selection-view {
  padding: 20px;
  max-width: 900px; /* Limit width for better layout */
  margin: 0 auto;
}

.el-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 16px;
  font-weight: bold;
}

.legend-header {
  flex-wrap: wrap; /* Allow legend to wrap on smaller screens */
}

/* Screening Info */
.screening-details {
  display: flex;
  align-items: flex-start;
}
.movie-poster-small {
  width: 80px;
  height: 120px;
  margin-right: 20px;
  flex-shrink: 0;
}
.info-text h3 {
  margin-top: 0;
  margin-bottom: 10px;
}
.info-text p {
  margin: 5px 0;
  display: flex;
  align-items: center;
  color: #606266;
}
.info-text .el-icon {
  margin-right: 5px;
}

/* Seat Legend */
.seat-legend {
  display: flex;
  flex-wrap: wrap;
  gap: 15px; /* Spacing between legend items */
  margin-top: 10px; /* Spacing from title */
}
.legend-item {
  display: flex;
  align-items: center;
  font-size: 12px;
}
.legend-item .seat {
  width: 20px;
  height: 20px;
  margin-right: 5px;
  display: inline-block;
  border-radius: 3px;
}

/* Seat Map */
.seat-map-container {
  margin-top: 20px;
  display: flex;
  flex-direction: column; /* Stack screen and rows */
  align-items: center; /* Center the map horizontally */
  overflow-x: auto; /* Allow horizontal scrolling if map is too wide */
  padding: 10px;
  background-color: #f9f9f9; /* Light background for the map area */
  border-radius: 4px;
}

.screen-indicator {
  width: 60%;
  margin-bottom: 25px;
  padding: 5px 0;
  text-align: center;
  background-color: #d3d3d3;
  color: #fff;
  font-size: 14px;
  border-radius: 2px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.seat-row {
  display: flex;
  margin-bottom: 8px; /* Spacing between rows */
  align-items: center;
}

.row-indicator {
  width: 40px; /* Fixed width for row indicator */
  text-align: right;
  margin-right: 10px;
  font-size: 12px;
  color: #909399;
}

.seat {
  width: 30px;
  height: 25px;
  margin: 0 4px; /* Spacing between seats */
  border-radius: 4px;
  cursor: pointer;
  transition: background-color 0.2s ease;
  display: inline-block; /* Use inline-block for proper spacing */
  border: 1px solid #ccc;
  box-sizing: border-box; /* Include border in size */
}

.seat.available {
  background-color: #fff;
  border-color: #b0d8ff;
}
.seat.available:hover {
  background-color: #e6f7ff;
}

.seat.selected {
  background-color: #fa4b2a; /* Highlight color for selected */
  border-color: #fa4b2a;
}

.seat.sold {
  background-color: #c0c4cc;
  border-color: #c0c4cc;
  cursor: not-allowed;
}

.seat.locked {
  /* Style if backend distinguishes locked */
  background-color: #e6a23c; /* Example: orange for locked */
  border-color: #e6a23c;
  cursor: not-allowed;
}

.seat.unavailable {
  background-color: #f0f0f0; /* Light grey background */
  border: 1px solid #e0e0e0; /* Slightly darker border than available */
  cursor: default;
  visibility: visible; /* Make it visible */
  /* Add diagonal lines using linear gradient */
  background-image: linear-gradient(
    45deg,
    rgba(180, 180, 180, 0.5) 25%,
    transparent 25%,
    transparent 50%,
    rgba(180, 180, 180, 0.5) 50%,
    rgba(180, 180, 180, 0.5) 75%,
    transparent 75%,
    transparent 100%
  );
  background-size: 8px 8px; /* Adjust size of the pattern */
}

/* Selection Summary */
.selection-summary-card .summary-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
}

.selection-summary-info {
  flex-grow: 1;
  margin-right: 20px; /* Space before price/button */
  min-height: 30px; /* Ensure height even when empty */
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 5px;
}

.selected-seat-tag {
  margin: 2px;
}

.no-seats-text {
  color: #909399;
  font-size: 14px;
}

.price-and-confirm {
  display: flex;
  align-items: center;
  flex-shrink: 0; /* Prevent shrinking */
}

.total-price {
  font-size: 16px;
  margin-right: 15px;
}

.price-value {
  font-weight: bold;
  color: #f56c6c; /* Price color */
  font-size: 18px;
}
</style>
