import axios, { type InternalAxiosRequestConfig, type AxiosError, type AxiosResponse } from 'axios'
import type {
  LoginPayload,
  RegisterPayload,
  LoginResponse,
  RegisterResponse,
  UserProfileDTO,
  UpdateProfileRequestDTO,
  ChangePasswordRequestDTO,
} from '../types/auth'
import type { Movie, MovieType } from '../types/movie'
import type { Page } from '../types/page'
import type { Screening, ScreeningDetailVO } from '../types/screening'
import type { Cinema } from '../types/cinema'
import type {
  SeatStatus,
  SeatIdentifier,
  SeatLock,
  LockSeatsRequest,
  UnlockSeatsRequest,
} from '@/types/seat'
import type { User } from '@/types/user'
import type { Order, OrderVO, CreateOrderRequest } from '@/types/order'
import type { Announcement, PublishRequest } from '@/types/announcement'
import type { MovieType as MovieTypeRelativePath } from '../types/movieType'
import type { Room } from '@/types/room'
import type { SummaryStatsDTO, ChartDataPointDTO, StatusDistributionDTO } from '@/types/stats'
import type { Rating } from '@/types/rating'

// 从环境变量或配置文件中获取后端 API 的基础 URL
// 这里暂时硬编码，后续可以优化为从配置读取
const API_BASE_URL = 'http://localhost:8080/api'

// 创建 Axios 实例
const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
})

// 请求拦截器：在每个请求发送前，检查本地存储中是否有 token
// 如果有，则添加到请求头的 Authorization 字段中
apiClient.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = localStorage.getItem('authToken') // 假设 token 存储在 localStorage
    if (token) {
      // Ensure headers object exists
      config.headers = config.headers || {}
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error: AxiosError) => {
    return Promise.reject(error)
  },
)

// 登录 API 调用
export const login = (loginData: LoginPayload) => {
  // Specify the expected response type for better type checking
  return apiClient.post<LoginResponse>('/auth/login', loginData)
}

// 注册 API 调用
export const register = (registerData: RegisterPayload) => {
  // Specify the expected response type
  return apiClient.post<RegisterResponse>('/auth/register', registerData)
}

// 获取电影列表 API 调用
// Define parameters type for clarity
interface FetchMoviesParams {
  current?: number
  size?: number
  title?: string
  status?: 'COMING_SOON' | 'NOW_PLAYING' | 'OFFLINE'
  // Assuming backend supports sort, add it. Otherwise, remove or handle sorting client-side.
  sort?: string
}
export const fetchMovies = (params: FetchMoviesParams) => {
  // The backend expects page number as 'current', not 'page'
  return apiClient.get<Page<Movie>>('/movies', { params })
}

/**
 * Fetches the details of a specific movie by its ID.
 * @param id - The ID of the movie.
 * @returns A promise that resolves to the API response containing the movie details.
 */
export const getMovieById = (id: number | string) => {
  return apiClient.get<Movie>(`/movies/${id}`)
}

/**
 * Fetches the earliest available screening date for a specific movie.
 * Returns date string "YYYY-MM-DD" on success, null if no future screenings found (404).
 * @param movieId - The ID of the movie.
 * @returns A promise that resolves to the API response containing the date string or null.
 */
export const getFirstScreeningDate = (
  movieId: string | number,
): Promise<AxiosResponse<string | null>> => {
  return apiClient
    .get<string>(`/movies/${movieId}/first-screening-date`)
    .catch((error: AxiosError) => {
      // Check if the error is an AxiosError and has a response with status 404
      if (error.response && error.response.status === 404) {
        // Return a successful-like response structure but with null data
        return Promise.resolve({
          data: null,
          status: error.response.status,
          statusText: error.response.statusText,
          headers: error.response.headers,
          config: error.response.config,
          request: error.request,
        } as AxiosResponse<null>) // Cast to AxiosResponse<null>
      }
      // For any other error, re-throw it
      throw error
    })
}

/**
 * Fetches movie rankings.
 * @param type - Ranking type: 'hot' (by rating) or 'upcoming' (by popularity).
 * @param limit - Number of movies to return (default: 10).
 * @returns A promise that resolves to the API response containing a list of movies.
 */
export const getMovieRankings = (
  type: 'hot' | 'upcoming',
  limit: number = 10,
): Promise<AxiosResponse<Movie[]>> => {
  return apiClient.get<Movie[]>(`/movies/rankings`, { params: { type, limit } })
}

/**
 * Fetches available screenings.
 * Can filter by movieId + date OR cinemaId + date.
 * @param params - Parameters including date, and EITHER movieId OR cinemaId.
 * @returns A promise that resolves to the API response containing a page of screenings.
 */
export const fetchAvailableScreenings = (params: {
  date: string // Format: yyyy-MM-dd
  movieId?: number | string // Make movieId optional
  cinemaId?: number | string // Optional cinema filter
  current?: number
  size?: number
}) => {
  // Basic validation (optional, backend should handle robustly)
  if (!params.movieId && !params.cinemaId) {
    console.warn('fetchAvailableScreenings requires either movieId or cinemaId')
    // Consider returning a rejected promise or empty page
  }
  return apiClient.get<Page<Screening>>('/screenings', { params })
}

/**
 * Fetches a list of approved cinemas (public view).
 * @param params - Parameters for pagination (current, size) and filtering (name, location, feature).
 * @returns A promise that resolves to the API response containing a page of approved cinemas.
 */
export const fetchApprovedCinemas = (params: {
  current?: number
  size?: number
  name?: string
  location?: string
  feature?: string
}) => {
  return apiClient.get<Page<Cinema>>('/cinemas', { params })
}

/**
 * Fetches the details of a specific approved cinema by its ID (public view).
 * @param id - The ID of the cinema.
 * @returns A promise that resolves to the API response containing the cinema details.
 */
export const getCinemaById = (id: number | string) => {
  // Ensure we are calling the public endpoint for approved cinemas
  return apiClient.get<Cinema>(`/cinemas/${id}`)
}

// --- Screening & Seat Selection APIs ---

/**
 * 获取单个场次详情 (用于选座页面)
 * @param screeningId 场次 ID
 * @returns Promise<ScreeningDetailVO>
 */
export const getScreeningDetails = async (screeningId: number): Promise<ScreeningDetailVO> => {
  const response = await apiClient.get<ScreeningDetailVO>(`/screenings/${screeningId}`)
  return response.data
}

/**
 * 获取场次座位状态图
 * @param screeningId 场次 ID
 * @returns Promise<SeatStatus[][]> 直接返回二维座位状态数组
 */
export const getScreeningSeatStatus = async (screeningId: number): Promise<SeatStatus[][]> => {
  // Assuming the backend ACTUALLY returns SeatStatus[][] despite the doc saying string
  const response = await apiClient.get<SeatStatus[][]>(`/screenings/${screeningId}/seats`)
  return response.data
}

/**
 * Lock seats for a screening.
 * @param screeningId - The ID of the screening.
 * @param seatsToLock - An array of SeatIdentifier objects (1-based index).
 * @returns Promise<AxiosResponse<SeatLock>> - Details of the lock created (or first lock if multiple).
 */
export const lockSeats = (
  screeningId: number,
  seatsToLock: SeatIdentifier[],
): Promise<AxiosResponse<SeatLock>> => {
  const data: LockSeatsRequest = { seatsToLock }
  // The API doc example response is a single SeatLock, adjust if it returns an array
  return apiClient.post<SeatLock>(`/screenings/${screeningId}/lock-seats`, data)
}

/**
 * Unlock previously locked seats for a screening.
 * @param screeningId - The ID of the screening.
 * @param seatsToUnlock - An array of SeatIdentifier objects (1-based index).
 * @returns Promise<AxiosResponse<void>> - Resolves on successful unlock (204 No Content).
 */
export const unlockSeats = (
  screeningId: number,
  seatsToUnlock: SeatIdentifier[],
): Promise<AxiosResponse<void>> => {
  const data: UnlockSeatsRequest = { seatsToUnlock }
  return apiClient.delete<void>(`/screenings/${screeningId}/unlock-seats`, {
    data: data, // Send payload in body for DELETE request
  })
}

// --- End Screening & Seat Selection APIs ---

// === ORDERS ===

/**
 * 创建新订单
 * @param data 创建订单所需的数据
 * @returns 订单详情视图对象
 */
export const createOrder = (data: CreateOrderRequest): Promise<AxiosResponse<OrderVO>> => {
  return apiClient.post<OrderVO>('/orders', data)
}

/**
 * (模拟)标记订单为已支付
 * @param identifier 订单ID或订单号
 * @returns 更新后的订单详情
 */
export const markOrderAsPaid = (identifier: string | number): Promise<AxiosResponse<Order>> => {
  return apiClient.put<Order>(`/orders/${identifier}/mark-as-paid`)
}

/**
 * Get order details by ID or order number.
 * @param identifier - The order ID or order number.
 * @returns Promise<AxiosResponse<Order>> - The order details.
 */
export const getOrderDetails = (identifier: string | number): Promise<AxiosResponse<Order>> => {
  return apiClient.get<Order>(`/orders/${identifier}`)
}

/**
 * Cancel an order.
 * Only orders in PENDING_PAYMENT status can be cancelled.
 * @param identifier - The order ID or order number to cancel.
 * @returns Promise<AxiosResponse<void>> - Empty response on success.
 */
export const cancelOrder = (identifier: string | number): Promise<AxiosResponse<void>> => {
  return apiClient.put<void>(`/orders/${identifier}/cancel`)
}

// New function for listing user's orders
/**
 * Fetches a paginated list of the current user's orders.
 * @param params - Optional parameters for pagination and filtering.
 * @param params.current - The current page number (defaults to 1).
 * @param params.size - The number of items per page (defaults to 10).
 * @param params.status - Filter by order status (optional).
 * @returns A promise that resolves to the paginated list of orders (OrderVO).
 */
export const listMyOrders = (params?: {
  current?: number
  size?: number
  status?: 'PENDING_PAYMENT' | 'PAID' | 'CANCELLED' | 'REFUNDED' | 'COMPLETED' | null
}): Promise<AxiosResponse<Page<OrderVO>>> => {
  // Assuming response structure is Page<OrderVO>
  return apiClient.get<Page<OrderVO>>('/orders', { params })
}

// === ANNOUNCEMENTS ===
/**
 * 获取已发布的公告列表（公开API）
 * @param params 分页参数
 * @returns 已发布的公告列表
 */
export const listPublishedAnnouncements = (params?: {
  current?: number
  size?: number
}): Promise<AxiosResponse<Page<Announcement>>> => {
  return apiClient.get<Page<Announcement>>('/announcements', { params })
}

// === ADMIN APIs (Example Structure) ===

// === CINEMA ADMIN APIs (Example Structure) ===

// --- Rooms ---
// getRoomById, updateRoom, deleteRoom, listMyRooms, addRoom ... (Keep existing if any)

export const listMyRooms = (): Promise<AxiosResponse<Room[]>> => {
  // Correct path: Remove leading /api as baseURL already contains it.
  return apiClient.get<Room[]>('/cinema-admin/rooms')
}

// ... Add other Room management functions if needed ...

// --- Screenings ---
// listMyScreenings, applyForScreening, cancelScreening ... (Keep existing)

interface ListMyScreeningsParams {
  current?: number
  size?: number
  movieId?: number
  date?: string // yyyy-MM-dd
  status?: 'PENDING_APPROVAL' | 'APPROVED' | 'REJECTED' | 'FINISHED' | 'CANCELLED'
}

/**
 * 获取影院管理员自己的场次列表
 */
export const listMyScreenings = (
  params?: ListMyScreeningsParams,
): Promise<AxiosResponse<Page<Screening>>> => {
  // Correct path: Remove leading /api
  return apiClient.get<Page<Screening>>('/cinema-admin/screenings', { params })
}

interface ApplyScreeningRequest {
  roomId: number
  movieId: number
  startTime: string // ISO 8601 format date-time string
  price: number
}

/**
 * 影院管理员申请新场次
 */
export const applyForScreening = (
  screeningData: ApplyScreeningRequest,
): Promise<AxiosResponse<Screening>> => {
  // Correct path: Remove leading /api
  return apiClient.post<Screening>('/cinema-admin/screenings', screeningData)
}

/**
 * 影院管理员取消场次
 */
export const cancelScreening = (screeningId: number): Promise<AxiosResponse<void>> => {
  // Correct path: Remove leading /api
  return apiClient.delete<void>(`/cinema-admin/screenings/${screeningId}`)
}

// --- Orders (NEW) ---

// Define parameters type for listing cinema orders
interface ListCinemaOrdersParams {
  current?: number
  size?: number
  status?: 'PENDING_PAYMENT' | 'PAID' | 'CANCELLED' | 'REFUNDED' | 'COMPLETED'
  // Add other potential filters as needed based on API spec
  // startDate?: string;
  // endDate?: string;
}

/**
 * 获取影院管理员自己影院的订单列表
 */
export const listCinemaOrders = (
  params: ListCinemaOrdersParams,
): Promise<AxiosResponse<Page<OrderVO>>> => {
  // Path based on API doc: /api/cinema-admin/orders
  // Correct path: Remove leading /api
  return apiClient.get<Page<OrderVO>>('/cinema-admin/orders', { params })
}

/**
 * 获取影院管理员自己影院的单个订单详情
 */
export const getCinemaOrderDetails = (
  identifier: string | number,
): Promise<AxiosResponse<OrderVO>> => {
  // Path based on API doc: /api/cinema-admin/orders/{identifier}
  // Correct path: Remove leading /api
  return apiClient.get<OrderVO>(`/cinema-admin/orders/${identifier}`)
}

// === END CINEMA ADMIN API ===

// === PROFILE ===

/**
 * 获取当前用户个人资料
 */
export const getUserProfile = (): Promise<AxiosResponse<UserProfileDTO>> => {
  return apiClient.get<UserProfileDTO>('/profile')
}

/**
 * 更新当前用户个人资料
 * @param data 包含要更新字段的对象
 */
export const updateUserProfile = (
  data: UpdateProfileRequestDTO,
): Promise<AxiosResponse<UserProfileDTO>> => {
  return apiClient.put<UserProfileDTO>('/profile', data)
}

/**
 * 修改当前用户密码
 * @param data 包含当前密码、新密码和确认密码的对象
 */
export const changePassword = (data: ChangePasswordRequestDTO): Promise<AxiosResponse<void>> => {
  // 204 No Content on success
  return apiClient.put<void>('/profile/password', data)
}

// === MOVIE TYPES (Admin) ===

/**
 * Fetches all movie types.
 */
export const getMovieTypes = (): Promise<AxiosResponse<MovieTypeRelativePath[]>> => {
  // Path already corrected
  return apiClient.get<MovieTypeRelativePath[]>('/admin/movie-types')
}

/**
 * Adds a new movie type.
 */
export const addMovieType = (
  typeData: Omit<MovieTypeRelativePath, 'id'>,
): Promise<AxiosResponse<MovieTypeRelativePath>> => {
  // Correct path: Remove leading /api
  return apiClient.post<MovieTypeRelativePath>('/admin/movie-types', typeData)
}

/**
 * Updates an existing movie type.
 */
export const updateMovieType = (
  id: number,
  typeData: Partial<Omit<MovieTypeRelativePath, 'id'>>,
): Promise<AxiosResponse<MovieTypeRelativePath>> => {
  // Correct path: Remove leading /api
  return apiClient.put<MovieTypeRelativePath>(`/admin/movie-types/${id}`, typeData)
}

/**
 * Deletes a movie type.
 */
export const deleteMovieType = (id: number): Promise<AxiosResponse<void>> => {
  // Correct path: Remove leading /api
  return apiClient.delete<void>(`/admin/movie-types/${id}`)
}

// === MOVIES (Admin) ===

/**
 * Fetches a paginated list of movies for admin view.
 * Assuming /movies endpoint handles admin access via JWT
 * (Path already corrected)
 */
export const adminListMovies = (params?: {
  current?: number
  size?: number
  title?: string
  status?: 'COMING_SOON' | 'NOW_PLAYING' | 'OFFLINE' | null
  movieTypeId?: number | null
}): Promise<AxiosResponse<Page<Movie>>> => {
  return apiClient.get<Page<Movie>>('/movies', { params })
}

/**
 * Adds a new movie.
 */
export const addMovie = (movieData: Movie): Promise<AxiosResponse<Movie>> => {
  // Ensure movieTypeIds is present in the data sent
  // Correct path: Remove leading /api
  return apiClient.post<Movie>('/admin/movies', movieData)
}

/**
 * Updates an existing movie.
 */
export const updateMovie = (id: number, movieData: Movie): Promise<AxiosResponse<Movie>> => {
  // Ensure movieTypeIds is present in the data sent
  // Correct path: Remove leading /api
  return apiClient.put<Movie>(`/admin/movies/${id}`, movieData)
}

/**
 * Deletes a movie.
 */
export const deleteMovie = (id: number): Promise<AxiosResponse<void>> => {
  // Correct path: Remove leading /api
  return apiClient.delete<void>(`/admin/movies/${id}`)
}

// === System Admin: User Management ===

export const listAdminUsers = (params: ListAdminUsersParams) => {
  return apiClient.get<Page<User>>('/admin/users', { params })
}

export const getAdminUserById = (id: number) => {
  return apiClient.get<User>(`/admin/users/${id}`)
}

export const updateAdminUser = (id: number, data: Partial<User>) => {
  return apiClient.put<User>(`/admin/users/${id}`, data)
}

export const deleteAdminUser = (id: number) => {
  return apiClient.delete<void>(`/admin/users/${id}`)
}

// === System Admin: Cinema Management ===

export const listAdminCinemas = (params: ListAdminCinemasParams) => {
  return apiClient.get<Page<Cinema>>('/admin/cinemas', { params })
}

export const getAdminCinemaById = (id: number) => {
  return apiClient.get<Cinema>(`/admin/cinemas/${id}`)
}

export const reviewCinema = (id: number, data: CinemaReviewRequest) => {
  return apiClient.put<Cinema>(`/admin/cinemas/${id}/review`, data)
}

// Use Partial<Cinema> as payload might not contain all fields
export const updateAdminCinema = (id: number, data: Partial<Cinema>) => {
  return apiClient.put<Cinema>(`/admin/cinemas/${id}`, data)
}

export const updateCinemaStatus = (id: number, data: CinemaStatusUpdateRequest) => {
  return apiClient.put<void>(`/admin/cinemas/${id}/status`, data) // API returns 204 No Content
}

// === System Admin: Screening Management ===

export const listAdminScreenings = (params: ListAdminScreeningsParams) => {
  // API doc indicates this endpoint returns Page<ScreeningDetailVO>
  return apiClient.get<Page<ScreeningDetailVO>>('/admin/screenings', { params })
}

export const reviewScreening = (id: number, data: ScreeningReviewRequest) => {
  // API doc indicates this returns the updated Screening
  return apiClient.put<Screening>(`/admin/screenings/${id}/review`, data)
}

// === SYSTEM ADMIN - ANNOUNCEMENTS ===

/**
 * Parameters for listing announcements for admin.
 */
export interface ListAdminAnnouncementsParams {
  current?: number
  size?: number
  isPublished?: boolean // true for published, false for unpublished
}

/**
 * Fetches a list of announcements for the admin.
 * Allows filtering by publication status.
 * @param params - Pagination and filtering parameters.
 * @returns A promise that resolves to the API response containing a page of announcements.
 */
export const listAdminAnnouncements = (
  params?: ListAdminAnnouncementsParams,
): Promise<AxiosResponse<Page<Announcement>>> => {
  return apiClient.get<Page<Announcement>>('/admin/announcements', { params })
}

/**
 * Adds a new announcement.
 * The new announcement defaults to unpublished status.
 * @param announcementData - Object containing title and content.
 * @returns A promise resolving to the created announcement.
 */
export const addAnnouncement = (
  announcementData: Pick<Announcement, 'title' | 'content'>,
): Promise<AxiosResponse<Announcement>> => {
  return apiClient.post<Announcement>('/admin/announcements', announcementData)
}

/**
 * Fetches the details of a specific announcement by its ID for admin.
 * @param id - The ID of the announcement.
 * @returns A promise resolving to the announcement details.
 */
export const getAdminAnnouncementById = (id: number): Promise<AxiosResponse<Announcement>> => {
  return apiClient.get<Announcement>(`/admin/announcements/${id}`)
}

/**
 * Updates the title and content of an existing announcement.
 * @param id - The ID of the announcement to update.
 * @param announcementData - Object containing the new title and/or content.
 * @returns A promise resolving to the updated announcement.
 */
export const updateAnnouncement = (
  id: number,
  announcementData: Pick<Announcement, 'title' | 'content'>,
): Promise<AxiosResponse<Announcement>> => {
  return apiClient.put<Announcement>(`/admin/announcements/${id}`, announcementData)
}

/**
 * Deletes an announcement by its ID.
 * @param id - The ID of the announcement to delete.
 * @returns A promise that resolves when the announcement is deleted.
 */
export const deleteAnnouncement = (id: number): Promise<AxiosResponse<void>> => {
  return apiClient.delete<void>(`/admin/announcements/${id}`)
}

/**
 * Publishes or unpublishes an announcement.
 * @param id - The ID of the announcement.
 * @param data - Object containing the publish status ({ publish: boolean }).
 * @returns A promise resolving to the updated announcement.
 */
export const publishAnnouncement = (
  id: number,
  data: PublishRequest,
): Promise<AxiosResponse<Announcement>> => {
  return apiClient.put<Announcement>(`/admin/announcements/${id}/publish`, data)
}

// === END SYSTEM ADMIN - ANNOUNCEMENTS ===

// === SYSTEM ADMIN - STATISTICS ===

/**
 * Fetches the summary statistics for the admin dashboard.
 * GET /api/admin/stats/summary
 */
export const getSummaryStatistics = (): Promise<AxiosResponse<SummaryStatsDTO>> => {
  return apiClient.get<SummaryStatsDTO>('/admin/stats/summary')
}

/**
 * Fetches the order trend data for the specified number of days.
 * GET /api/admin/stats/order-trend
 * @param days - The number of days to fetch the trend for (e.g., 7 or 30).
 */
export const getOrderTrend = (days: number = 7): Promise<AxiosResponse<ChartDataPointDTO[]>> => {
  return apiClient.get<ChartDataPointDTO[]>('/admin/stats/order-trend', { params: { days } })
}

/**
 * Fetches the top N movies by box office revenue.
 * GET /api/admin/stats/movie-box-office
 * @param limit - The maximum number of movies to return (e.g., 5 or 10).
 */
export const getTopMovieBoxOffice = (
  limit: number = 5,
): Promise<AxiosResponse<ChartDataPointDTO[]>> => {
  return apiClient.get<ChartDataPointDTO[]>('/admin/stats/movie-box-office', { params: { limit } })
}

/**
 * Fetches the distribution of cinemas by their status.
 * GET /api/admin/stats/cinema-status-distribution
 */
export const getCinemaStatusDistribution = (): Promise<AxiosResponse<StatusDistributionDTO[]>> => {
  return apiClient.get<StatusDistributionDTO[]>('/admin/stats/cinema-status-distribution')
}

// === END SYSTEM ADMIN - STATISTICS ===

// === Helper Types ===

// Interface for listAdminCinemas params
export interface ListAdminCinemasParams {
  current?: number
  size?: number
  status?: 'PENDING_APPROVAL' | 'APPROVED' | 'REJECTED' | 'DISABLED'
}

// Interface for listAdminUsers params
export interface ListAdminUsersParams {
  current?: number
  size?: number
  username?: string
  role?: 'USER' | 'CINEMA_ADMIN' | 'SYSTEM_ADMIN'
}

// Interface for reviewCinema payload (based on API doc)
export interface CinemaReviewRequest {
  approved: boolean
  adminUserId?: number // Optional user ID to associate
}

// Interface for updateCinemaStatus payload (based on API doc)
export interface CinemaStatusUpdateRequest {
  status: 'PENDING_APPROVAL' | 'APPROVED' | 'REJECTED' | 'DISABLED'
}

// Interface for listAdminScreenings params (NEW)
export interface ListAdminScreeningsParams {
  current?: number
  size?: number
  cinemaId?: number
  movieId?: number
  date?: string // yyyy-MM-dd
  status?: 'PENDING_APPROVAL' | 'APPROVED' | 'REJECTED' | 'FINISHED' | 'CANCELLED'
}

// Interface for reviewScreening payload (NEW)
export interface ScreeningReviewRequest {
  approved: boolean
}

// === 电影收藏相关 API ===

/**
 * 收藏电影
 * @param movieId 电影ID
 * @returns 操作结果
 */
export const addFavorite = (movieId: string | number): Promise<AxiosResponse<void>> => {
  return apiClient.post<void>(`/movies/${movieId}/favorite`)
}

/**
 * 取消收藏电影
 * @param movieId 电影ID
 * @returns 操作结果
 */
export const removeFavorite = (movieId: string | number): Promise<AxiosResponse<void>> => {
  return apiClient.delete<void>(`/movies/${movieId}/favorite`)
}

/**
 * 获取当前用户对电影的收藏状态
 * @param movieId 电影ID
 * @returns 是否已收藏
 */
export const getFavoriteStatus = (
  movieId: string | number,
): Promise<AxiosResponse<{ isFavorited: boolean }>> => {
  return apiClient.get<{ isFavorited: boolean }>(`/movies/${movieId}/favorite/status`)
}

/**
 * 获取当前用户的收藏电影列表
 * @param params 分页参数
 * @returns 收藏的电影列表
 */
export const getMyFavorites = (params?: {
  current?: number
  size?: number
}): Promise<AxiosResponse<Page<Movie>>> => {
  return apiClient.get<Page<Movie>>('/profile/favorites', { params })
}

// === 电影评分相关 API ===

/**
 * 对电影进行评分或更新评分
 * @param movieId 电影ID
 * @param data 评分数据
 * @returns 评分结果
 */
export const rateMovie = (
  movieId: string | number,
  data: { score: number; comment?: string },
): Promise<AxiosResponse<Rating>> => {
  return apiClient.post<Rating>(`/movies/${movieId}/rate`, data)
}

/**
 * 获取当前用户对某电影的评分
 * @param movieId 电影ID
 * @returns 用户评分信息
 */
export const getMyRating = (movieId: string | number): Promise<AxiosResponse<Rating>> => {
  return apiClient.get<Rating>(`/movies/${movieId}/rating/my`)
}

/**
 * 获取电影的评分列表
 * @param movieId 电影ID
 * @param page 页码
 * @param size 每页数量
 * @returns 评分列表
 */
export const getMovieRatings = (
  movieId: string | number,
  page: number = 1,
  size: number = 10,
): Promise<AxiosResponse<Page<Rating>>> => {
  return apiClient.get<Page<Rating>>(`/movies/${movieId}/ratings`, {
    params: { current: page, size },
  })
}

/**
 * 获取公共电影类型列表（无需管理员权限）
 * @returns Promise<AxiosResponse<MovieTypeRelativePath[]>>
 */
export const getPublicMovieTypes = (): Promise<AxiosResponse<MovieTypeRelativePath[]>> => {
  return apiClient.get<MovieTypeRelativePath[]>('/movie-types')
}

// === Cinema Partnership Application ===

/**
 * 定义提交影院合作申请的请求体类型
 */
export interface CinemaPartnershipApplicationRequestDTO {
  name: string
  address: string
  phone?: string
  logo?: string
  description?: string
}

/**
 * 提交影院合作申请
 * @param data 申请信息
 * @returns 提交结果
 */
export const submitCinemaApplication = (
  data: CinemaPartnershipApplicationRequestDTO,
): Promise<AxiosResponse<Cinema>> => {
  return apiClient.post<Cinema>('/cinemas/partnership-application', data)
}

// --- Payment APIs ---

/**
 * 创建支付宝支付订单
 * @param orderNo 订单号
 * @param frontendUrl 前端应用的基础URL，用于支付完成后的重定向
 * @returns 支付宝表单HTML字符串
 */
export const createAlipayPayment = (
  orderNo: string,
  frontendUrl?: string,
): Promise<AxiosResponse<string>> => {
  const params = frontendUrl ? { frontendUrl } : undefined
  return apiClient.post<string>(`/payment/alipay/create/${orderNo}`, null, {
    responseType: 'text',
    params,
  })
}

/**
 * 定义查询支付状态接口的响应类型
 */
export interface AlipayQueryResponse {
  orderNo: string
  tradeStatus?: string // 支付宝交易状态 (e.g., TRADE_SUCCESS, WAIT_BUYER_PAY)
  isPaid: boolean // 后端确认的支付状态
  paymentTime?: string // 支付时间 (ISO 8601)
}

/**
 * 查询支付宝订单支付状态
 * @param orderNo 订单号
 * @returns 支付状态信息
 */
export const queryAlipayPaymentStatus = (
  orderNo: string,
): Promise<AxiosResponse<AlipayQueryResponse>> => {
  return apiClient.get<AlipayQueryResponse>(`/payment/alipay/query/${orderNo}`)
}

// --- End Payment APIs ---

export default apiClient
