/**
 * 影厅信息接口
 */
export interface Room {
  id: number
  cinemaId: number
  name: string
  rows: number // 行数
  cols: number // 列数
  seatTemplate?: string // 座位模板 (可选)
  createTime?: string // 创建时间 (ISO 8601 string)
  updateTime?: string // 更新时间 (ISO 8601 string)
}
