import api from './api' // 导入配置好的 axios 实例
import type { Cinema } from '@/types/cinema' // 导入 Cinema 类型
import type { Room } from '@/types/room' // Import Room type

/**
 * 获取当前影院管理员管理的影院信息
 * @returns Promise<Cinema> 影院信息
 */
export const getMyCinema = async (): Promise<Cinema> => {
  const response = await api.get<Cinema>('/cinema-admin/cinema')
  return response.data
}

/**
 * 更新当前影院管理员管理的影院信息
 * @param cinemaData 要更新的影院信息
 * @returns Promise<Cinema> 更新后的影院信息
 */
export const updateMyCinema = async (cinemaData: Cinema): Promise<Cinema> => {
  // 注意：后端可能只允许更新部分字段，并且会校验 cinemaData.id
  const response = await api.put<Cinema>('/cinema-admin/cinema', cinemaData)
  return response.data
}

/**
 * 获取当前管理员影院的影厅列表
 * @returns Promise<Room[]> 影厅列表
 */
export const listMyRooms = async (): Promise<Room[]> => {
  const response = await api.get<Room[]>('/cinema-admin/rooms')
  return response.data
}

/**
 * 添加新影厅
 * @param roomData 新影厅信息 (name, rows, cols)
 * @returns Promise<Room> 创建成功的影厅信息
 */
export const addRoom = async (
  roomData: Omit<Room, 'id' | 'cinemaId' | 'createTime' | 'updateTime'>,
): Promise<Room> => {
  const response = await api.post<Room>('/cinema-admin/rooms', roomData)
  return response.data
}

/**
 * 更新影厅信息
 * @param roomId 要更新的影厅 ID
 * @param roomData 更新后的影厅信息 (只需要 name, rows, cols)
 * @returns Promise<Room> 更新后的影厅信息
 */
export const updateRoom = async (
  roomId: number,
  roomData: Pick<Room, 'name' | 'rows' | 'cols'>,
): Promise<Room> => {
  const response = await api.put<Room>(`/cinema-admin/rooms/${roomId}`, roomData)
  return response.data
}

/**
 * 删除影厅
 * @param roomId 要删除的影厅 ID
 * @returns Promise<void>
 */
export const deleteRoom = async (roomId: number): Promise<void> => {
  await api.delete(`/cinema-admin/rooms/${roomId}`)
}
