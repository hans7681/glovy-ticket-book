/**
 * 座位标识符，用于锁定/解锁/订单请求
 */
export interface SeatIdentifier {
  rowIndex: number // 行号 (从1开始)
  colIndex: number // 列号 (从1开始)
}

/**
 * 座位状态信息 (用于选座图)
 */
export interface SeatStatus extends SeatIdentifier {
  status: 'AVAILABLE' | 'SOLD' | 'LOCKED' | 'UNAVAILABLE' | 'SELECTED' // SELECTED 是前端临时状态
  seatLabel?: string // 可选的座位标签，如 "5排8座"
}

/**
 * 获取座位状态接口的响应体结构 (假设)
 */
export interface SeatStatusResponse {
  rows: number
  cols: number
  seats: SeatStatus[] // 扁平化的座位状态列表
}

/**
 * 锁定座位请求体
 */
export interface LockSeatsRequest {
  seatsToLock: SeatIdentifier[]
}

/**
 * 解锁座位请求体
 */
export interface UnlockSeatsRequest {
  seatsToUnlock: SeatIdentifier[]
}

/**
 * 座位锁定记录 (锁定API成功响应)
 */
export interface SeatLock {
  id: number
  screeningId: number
  rowIndex: number
  colIndex: number
  userId: number
  lockExpiryTime: string // ISO 8601 string
  createTime: string // ISO 8601 string
}
