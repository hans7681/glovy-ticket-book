import api from './api' // Assuming your configured axios instance is exported from './api'
import type { User } from '@/types/user' // Assuming you have a User type
import type { Page } from '@/types/page' // Assuming you have a Page type

interface GetUsersParams {
  current?: number
  size?: number
  username?: string
  role?: 'USER' | 'CINEMA_ADMIN' | 'SYSTEM_ADMIN' | ''
}

// Fetch user list
export const getUsers = async (params: GetUsersParams): Promise<Page<User>> => {
  try {
    const response = await api.get<Page<User>>('/admin/users', { params })
    return response.data
  } catch (error) {
    console.error('Error fetching users:', error)
    // Return a default empty page structure on error or re-throw
    return {
      records: [],
      total: 0,
      size: params.size || 10,
      current: params.current || 1,
      pages: 0,
    }
    // Or: throw error;
  }
}

// Update user information (excluding password)
export const updateUser = async (id: number, userData: Partial<User>): Promise<User> => {
  try {
    // Ensure password is not sent, backend should ignore it anyway based on API doc
    const dataToSend = { ...userData }
    delete dataToSend.password
    delete dataToSend.username // Username typically cannot be changed

    const response = await api.put<User>(`/admin/users/${id}`, dataToSend)
    return response.data
  } catch (error) {
    console.error(`Error updating user ${id}:`, error)
    throw error // Re-throw to be handled in the component
  }
}

// Delete a user (logical delete)
export const deleteUser = async (id: number): Promise<void> => {
  try {
    await api.delete(`/admin/users/${id}`)
  } catch (error) {
    console.error(`Error deleting user ${id}:`, error)
    throw error // Re-throw to be handled in the component
  }
}

// Add other admin API functions here (movies, cinemas, etc.) as needed
