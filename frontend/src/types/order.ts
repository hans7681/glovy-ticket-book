/**
 * Represents the identifier for a single seat.
 * Used in CreateOrderRequest.
 * Assumes 1-based indexing as required by the backend API.
 */
export interface SeatInfo {
  rowIndex: number // 1-based
  colIndex: number // 1-based
}

/**
 * Request body for creating a new order.
 */
export interface CreateOrderRequest {
  screeningId: number
  selectedSeats: SeatInfo[] // Array of 1-based seat identifiers
}

/**
 * Represents a single seat within an order.
 */
export interface OrderSeat {
  id: number
  orderId: number
  screeningId: number
  rowIndex: number // Usually 1-based from backend
  colIndex: number // Usually 1-based from backend
  seatLabel?: string // e.g., "5排7座"
}

/**
 * Represents the detailed Order object, often returned by GET /api/orders/{id}.
 */
export interface Order {
  id: number
  orderNo: string
  userId: number
  screeningId: number
  cinemaId: number
  movieId: number
  totalAmount: number // Renamed from totalPrice in example for consistency?
  seatCount: number
  status: 'PENDING_PAYMENT' | 'PAID' | 'CANCELLED' | 'REFUNDED' | 'COMPLETED'
  paymentTime?: string | null // ISO 8601 or null
  cancelTime?: string | null // ISO 8601 or null
  createTime: string // ISO 8601
  updateTime?: string // ISO 8601
  seats?: OrderSeat[] // Use OrderSeat type
  // Optional embedded details (adjust based on actual API response structure)
  movieTitle?: string
  cinemaName?: string
  roomName?: string
  screeningTime?: string // ISO 8601
}

/**
 * Represents the Order View Object, often returned after creating an order (POST /api/orders)
 * or in order lists.
 * Based on the OrderVO example in the API documentation.
 */
export interface OrderVO {
  // Rename orderId to id to match assumed API response and template usage
  id: number
  orderNo: string
  userId: number
  screeningId: number
  movieId: number
  movieTitle: string
  cinemaId: number
  cinemaName: string
  roomName: string
  screeningStartTime: string // ISO 8601
  totalAmount: number
  seatCount: number
  status: 'PENDING_PAYMENT' | 'PAID' | 'CANCELLED' | 'REFUNDED' | 'COMPLETED'
  paymentTime?: string | null // ISO 8601 or null
  cancelTime?: string | null // ISO 8601 or null
  createTime: string // ISO 8601
  seatsDescription?: string[] // e.g., ["5排7座", "5排8座"]
}
