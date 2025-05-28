import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { login as apiLogin } from '../services/api' // Corrected relative path
import type { LoginPayload, User, LoginResponse } from '../types/auth' // Corrected relative path
import type { AxiosResponse } from 'axios'
import { jwtDecode } from 'jwt-decode'

// Helper function to safely parse user data from localStorage
function parseUserData(): User | null {
  const userDataString = localStorage.getItem('userData')
  if (userDataString) {
    try {
      const parsed = JSON.parse(userDataString)
      // Basic validation to check if it resembles a User object
      if (parsed && typeof parsed === 'object') {
        return parsed as User
      }
    } catch (error) {
      console.error('Error parsing user data from localStorage:', error)
    }
  }
  return null
}

// Define expected JWT payload structure
interface JwtPayload {
  // userId: number; // Not present in actual token
  // username: string;
  sub: string // Standard JWT subject claim, used for username
  role: 'USER' | 'CINEMA_ADMIN' | 'SYSTEM_ADMIN' // Renamed from userRole
  exp?: number
  iat?: number // Add issuedAt if present and needed
  // Add other relevant fields if needed
}

export const useAuthStore = defineStore('auth', () => {
  const router = useRouter()

  // 从 localStorage 初始化 token
  const token = ref<string | null>(localStorage.getItem('authToken') || null)
  // 初始化 user 为 null
  const user = ref<User | null>(null)

  // Helper to set user state from decoded token or parsed data
  function setUserState(userData: User | null) {
    user.value = userData
    if (userData) {
      localStorage.setItem('userData', JSON.stringify(userData))
    } else {
      localStorage.removeItem('userData')
    }
  }

  // Function to initialize auth state from localStorage
  function initializeAuth() {
    const storedToken = localStorage.getItem('authToken')
    const storedUser = parseUserData() // Keep parsing for quick init if available

    token.value = storedToken

    if (storedToken) {
      try {
        const decoded = jwtDecode<JwtPayload>(storedToken)
        const isExpired = decoded.exp ? decoded.exp * 1000 < Date.now() : false

        if (!isExpired) {
          console.log('Initializing user state from stored token:', decoded)
          setUserState({
            id: null, // Assume ID is not in token
            username: decoded.sub,
            nickname: storedUser?.nickname || null,
            role: decoded.role,
          })
          console.log('User state set in initializeAuth. Current user.value:', user.value)
        } else {
          console.log('Stored token is expired. Clearing auth data.')
          setAuthData(null, null) // Clear expired token and user data
        }
      } catch (error) {
        console.error('Failed to decode stored token. Clearing auth data:', error)
        setAuthData(null, null) // Clear invalid token and user data
      }
    } else if (storedUser) {
      // If no token but user data exists (should not happen ideally),
      // maybe just load it but log a warning, or clear it.
      console.warn('User data found in localStorage without a token. Clearing user data.')
      setUserState(null) // Clear user data if no token
    }
    // If neither token nor user data is found, user remains null.
  }

  // Call initializeAuth when the store is created
  initializeAuth()

  // Modify isLoggedIn to primarily depend on the presence and validity of the token
  const isLoggedIn = computed(() => {
    if (!token.value) return false
    try {
      const decoded = jwtDecode<JwtPayload>(token.value)
      const isExpired = decoded.exp ? decoded.exp * 1000 < Date.now() : false
      return !isExpired // Logged in if token exists and is not expired
    } catch (error) {
      console.error('Failed to decode token for isLoggedIn check:', error)
      return false // Treat as logged out if token is invalid
    }
  })

  // 保存 token 和 user 到 localStorage
  function setAuthData(newToken: string | null, userData: User | null) {
    token.value = newToken
    // Use the dedicated setter function now
    setUserState(userData)
    if (newToken) {
      localStorage.setItem('authToken', newToken)
    } else {
      localStorage.removeItem('authToken')
    }
  }

  // 登录 Action
  async function login(payload: LoginPayload) {
    console.log('Attempting login with payload:', payload)
    try {
      console.log('Calling apiLogin...')
      const response = await apiLogin(payload)
      return setAuthDataFromResponse(response)
    } catch (error) {
      console.error('Login failed in catch block:', error)
      setAuthData(null, null)
      return false
    }
  }

  // 从登录响应设置认证数据
  async function setAuthDataFromResponse(response: AxiosResponse<LoginResponse>) {
    console.log('apiLogin response received:', response)
    const newToken = response.data.token

    console.log('Received token:', newToken)
    if (newToken) {
      console.log('Token received, attempting to decode...')
      try {
        const decoded = jwtDecode<JwtPayload>(newToken)
        console.log('Decoded JWT Payload:', decoded)

        const loggedInUser: User = {
          id: null, // Assume ID is not in token
          username: decoded.sub,
          nickname: null, // TODO
          role: decoded.role,
        }
        console.log('Created loggedInUser object:', loggedInUser)
        console.log('Calling setAuthData...')
        setAuthData(newToken, loggedInUser)
        console.log('setAuthData called. Current authStore.user:', user.value)

        if (loggedInUser.role === 'SYSTEM_ADMIN') {
          console.log('Redirecting to /admin...')
          await router.push('/admin')
        } else {
          console.log('Redirecting to /...')
          await router.push('/')
        }
        return true
      } catch (error) {
        console.error('Failed to decode token:', error)
        setAuthData(null, null)
        return false
      }
    } else {
      console.log('No token received in response.')
      setAuthData(null, null)
      return false
    }
  }

  // 登出 Action
  function logout() {
    setAuthData(null, null)
    // 跳转到登录页
    router.push('/login')
  }

  return { token, user, isLoggedIn, login, logout, setAuthData, setAuthDataFromResponse }
})
