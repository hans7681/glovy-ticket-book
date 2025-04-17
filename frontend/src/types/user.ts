export enum UserRole {
  USER = 'USER',
  CINEMA_ADMIN = 'CINEMA_ADMIN',
  SYSTEM_ADMIN = 'SYSTEM_ADMIN',
}

export interface User {
  id: number
  username: string
  password?: string // 密码通常不在列表或详情中返回，设为可选
  nickname: string | null
  avatar?: string | null // 头像可选
  phone: string | null
  email: string | null
  role: UserRole
  createTime: string // 使用 string 类型，具体格式根据后端返回确定
  updateTime: string // 使用 string 类型
}

// 用于更新用户信息时，通常不包含或忽略 username, password, createTime, updateTime
export interface UserUpdateRequest
  extends Omit<User, 'id' | 'username' | 'password' | 'createTime' | 'updateTime' | 'role'> {
  role?: UserRole // 角色可能允许管理员更新
}
