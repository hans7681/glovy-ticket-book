/**
 * Formats an ISO 8601 date-time string into a more readable format.
 * Example: '2023-10-27T10:30:00' -> '2023-10-27 10:30:00'
 * Adjust the format string as needed.
 * @param isoString - The ISO 8601 date-time string.
 * @returns Formatted date-time string, or the original string if input is invalid.
 */
export function formatDateTime(isoString: string | null | undefined): string {
  if (!isoString) {
    return '-' // Or return empty string, based on preference
  }
  try {
    const date = new Date(isoString)
    // Basic formatting, consider using a library like date-fns or dayjs for more options
    const year = date.getFullYear()
    const month = (date.getMonth() + 1).toString().padStart(2, '0')
    const day = date.getDate().toString().padStart(2, '0')
    const hours = date.getHours().toString().padStart(2, '0')
    const minutes = date.getMinutes().toString().padStart(2, '0')
    const seconds = date.getSeconds().toString().padStart(2, '0')
    return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`
  } catch (error) {
    console.error('Error formatting date:', isoString, error)
    return isoString // Return original string on error
  }
}

/**
 * Formats a price (number) into a currency string.
 * Example: 45 -> '¥45.00'
 * @param price - The price as a number.
 * @returns Formatted currency string.
 */
export function formatPrice(price: number | null | undefined): string {
  if (price === null || price === undefined) {
    return '-'
  }
  // You might want to use Intl.NumberFormat for better localization
  return `¥${price.toFixed(2)}`
}

// Add other formatters as needed (e.g., formatDuration)
