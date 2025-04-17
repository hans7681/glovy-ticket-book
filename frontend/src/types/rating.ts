import type { User } from './user'

/**
 * 电影评分实体类型
 */
export interface Rating {
  id: number
  userId: number
  movieId: number
  score: number // 评分 (1-10)
  comment?: string // 评论内容
  createTime?: string // 创建时间
  updateTime?: string // 更新时间
  user?: User // 用户信息（可能包含在返回结果中）
}
