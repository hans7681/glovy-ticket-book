/**
 * 格式化日期时间字符串
 * @param dateTimeString ISO 格式的日期时间字符串，或任何 Date 对象可以解析的字符串
 * @param format 目标格式 (默认为 'YYYY-MM-DD HH:mm:ss') - 注意：这里仅实现简单替换，非严格格式化
 * @returns 格式化后的字符串，如果输入无效则返回空字符串
 */
export function formatDateTime(
  dateTimeString: string | Date | undefined | null,
  format: string = 'YYYY-MM-DD HH:mm:ss',
): string {
  if (!dateTimeString) {
    return ''
  }

  try {
    const date = new Date(dateTimeString)
    if (isNaN(date.getTime())) {
      return '' // Invalid date
    }

    const year = date.getFullYear()
    const month = (date.getMonth() + 1).toString().padStart(2, '0')
    const day = date.getDate().toString().padStart(2, '0')
    const hours = date.getHours().toString().padStart(2, '0')
    const minutes = date.getMinutes().toString().padStart(2, '0')
    const seconds = date.getSeconds().toString().padStart(2, '0')

    let formattedString = format
    formattedString = formattedString.replace('YYYY', year.toString())
    formattedString = formattedString.replace('MM', month)
    formattedString = formattedString.replace('DD', day)
    formattedString = formattedString.replace('HH', hours)
    formattedString = formattedString.replace('mm', minutes)
    formattedString = formattedString.replace('ss', seconds)

    return formattedString
  } catch (e) {
    console.error('Error formatting date:', e)
    return '' // Return empty string on error
  }
}

/**
 * 获取未来 N 天的日期信息列表
 * @param days 天数 (默认 7)
 * @returns 包含日期标签和 YYYY-MM-DD 格式值的数组
 */
export function getFutureDateOptions(days: number = 7): { label: string; value: string }[] {
  const options: { label: string; value: string }[] = []
  const today = new Date()
  today.setHours(0, 0, 0, 0) // 标准化到当天的开始

  const weekdays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']

  for (let i = 0; i < days; i++) {
    const date = new Date(today)
    date.setDate(today.getDate() + i)

    const year = date.getFullYear()
    const month = (date.getMonth() + 1).toString().padStart(2, '0')
    const day = date.getDate().toString().padStart(2, '0')
    const dayOfWeek = weekdays[date.getDay()]

    const dateValue = `${year}-${month}-${day}`
    let dateLabel = ''

    if (i === 0) {
      dateLabel = `今天 (${dayOfWeek})`
    } else {
      dateLabel = `${month}/${day} (${dayOfWeek})`
    }

    options.push({ label: dateLabel, value: dateValue })
  }

  return options
}

/**
 * 将日期字符串格式化为 YYYY-MM-DD
 * @param date Date 对象或可被 Date 解析的字符串
 * @returns YYYY-MM-DD 格式的字符串，无效则返回空字符串
 */
export function formatDateToYYYYMMDD(date: Date | string | null | undefined): string {
  if (!date) return ''
  try {
    const d = typeof date === 'string' ? new Date(date) : date
    if (isNaN(d.getTime())) {
      return ''
    }
    const year = d.getFullYear()
    const month = (d.getMonth() + 1).toString().padStart(2, '0')
    const day = d.getDate().toString().padStart(2, '0')
    return `${year}-${month}-${day}`
  } catch (e) {
    console.error('Error formatting date to YYYY-MM-DD:', e)
    return ''
  }
}
