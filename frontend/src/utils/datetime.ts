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
