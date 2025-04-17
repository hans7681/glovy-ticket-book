// src/types/auth.ts

export interface LoginPayload {
  username: string
  password?: string // Password might be optional depending on context
}

export interface RegisterPayload {
  username: string
  password?: string
  nickname?: string
  phone?: string
  email?: string
  role?: 'USER' | 'CINEMA_ADMIN' | 'SYSTEM_ADMIN'
}

export interface LoginResponse {
  token: string
  // Add other fields if the API returns more user info on login
}

export interface RegisterResponse {
  message: string
  userId: number
  username: string
}

// 统一错误响应格式
export interface ErrorResponse {
  timestamp: string
  status: number
  error: string
  message: string
  path: string
  fieldErrors?: {
    field: string
    message: string
  }[]
}

// Interface for user data stored in Pinia store
export interface User {
  id: number | null
  username: string | null
  nickname: string | null
  role: 'USER' | 'CINEMA_ADMIN' | 'SYSTEM_ADMIN' | null
}

// Type returned by GET /api/profile
export interface UserProfileDTO {
  id: number | null // ID might not be in the token but present here
  username: string
  nickname: string | null
  avatar?: string | null // Optional avatar URL
  phone: string | null
  email: string | null
  role: 'USER' | 'CINEMA_ADMIN' | 'SYSTEM_ADMIN'
  createTime?: string // Optional timestamps
  updateTime?: string
}

// Payload for PUT /api/profile
export interface UpdateProfileRequestDTO {
  nickname?: string
  avatar?: string
  phone?: string
  email?: string
}

// Payload for PUT /api/profile/password
export interface ChangePasswordRequestDTO {
  currentPassword: string
  newPassword: string
  confirmPassword: string // Frontend validation checks if this matches newPassword
}
