export interface Screening {
  id: number
  movieId: number
  roomId: number
  cinemaId: number
  startTime: string // ISO 8601 date-time string
  endTime: string // ISO 8601 date-time string
  price: number
  status: 'PENDING_APPROVAL' | 'APPROVED' | 'REJECTED' | 'FINISHED' | 'CANCELLED'
  createTime?: string // Optional, as not always present in list views
  updateTime?: string // Optional

  // Potential fields added by backend (if backend joins data)
  movieTitle?: string
  cinemaName?: string
  roomName?: string
}

/**
 * 场次详情视图对象 (用于选座页)
 * 来自 GET /api/screenings/{screeningId}
 */
export interface ScreeningDetailVO {
  screeningId: number
  movieId: number
  movieTitle: string
  moviePosterUrl?: string // 可选
  movieDuration?: number // 可选
  cinemaId: number
  cinemaName: string
  roomId: number
  roomName: string
  startTime: string // ISO 8601 string
  endTime: string // ISO 8601 string
  price: number
  roomRowsCount?: number // 可选
  roomColsCount?: number // 可选
  status: 'PENDING_APPROVAL' | 'APPROVED' | 'REJECTED' | 'FINISHED' | 'CANCELLED'
}
