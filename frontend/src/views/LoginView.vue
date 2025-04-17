<script setup lang="ts">
import { ref } from 'vue'
import { login as apiLogin } from '@/services/api'
import type { ErrorResponse } from '@/types/auth'
import type { AxiosError } from 'axios'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()

const username = ref('')
const password = ref('')
const fieldErrors = ref<Record<string, string>>({})
const errorMessage = ref<string | null>(null)
const isLoading = ref(false)

// 表单验证
const validateForm = () => {
  fieldErrors.value = {}
  errorMessage.value = null

  let isValid = true

  // 用户名验证
  if (!username.value.trim()) {
    fieldErrors.value.username = '请输入用户名'
    isValid = false
  }

  // 密码验证
  if (!password.value) {
    fieldErrors.value.password = '请输入密码'
    isValid = false
  }

  return isValid
}

const handleLogin = async () => {
  if (!validateForm()) {
    return
  }

  isLoading.value = true

  try {
    // 直接调用API，而不是通过store，以便捕获详细错误
    const response = await apiLogin({
      username: username.value,
      password: password.value,
    })

    if (response.data && response.data.token) {
      // 登录成功，调用store的setAuthData方法
      const success = await authStore.setAuthDataFromResponse(response)
      if (success) {
        // 由store处理重定向
      } else {
        errorMessage.value = '登录后处理失败，请重试'
      }
    } else {
      errorMessage.value = '登录失败，响应格式不正确'
    }
  } catch (error) {
    console.error('Login failed:', error)

    // 处理后端返回的统一错误格式
    const axiosError = error as AxiosError<ErrorResponse>

    if (axiosError.response?.data) {
      const errorData = axiosError.response.data

      // 处理字段级别的错误
      if (errorData.fieldErrors && errorData.fieldErrors.length > 0) {
        errorData.fieldErrors.forEach((fieldError) => {
          fieldErrors.value[fieldError.field] = fieldError.message
        })
        errorMessage.value = '请修正表单中的错误后重试'
      } else if (axiosError.response.status === 401) {
        // 处理凭据无效的情况
        errorMessage.value = '用户名或密码错误，请重试'
      } else {
        // 处理其他错误
        errorMessage.value = errorData.message || '登录失败，请稍后重试'
      }
    } else {
      errorMessage.value = '登录失败，请检查网络连接后重试'
    }
  } finally {
    isLoading.value = false
  }
}
</script>

<template>
  <div class="login-view">
    <h2>用户登录</h2>
    <form @submit.prevent="handleLogin" class="login-form">
      <div class="form-group" :class="{ 'has-error': fieldErrors.username }">
        <label for="username">用户名</label>
        <input type="text" id="username" v-model="username" required placeholder="请输入用户名" />
        <div class="field-error" v-if="fieldErrors.username">{{ fieldErrors.username }}</div>
      </div>

      <div class="form-group" :class="{ 'has-error': fieldErrors.password }">
        <label for="password">密码</label>
        <input type="password" id="password" v-model="password" required placeholder="请输入密码" />
        <div class="field-error" v-if="fieldErrors.password">{{ fieldErrors.password }}</div>
      </div>

      <div v-if="errorMessage" class="error-message">
        {{ errorMessage }}
      </div>

      <button type="submit" class="btn-submit" :disabled="isLoading">
        {{ isLoading ? '登录中...' : '登录' }}
      </button>

      <p class="register-link">还没有账号？ <RouterLink to="/register">立即注册</RouterLink></p>
    </form>
  </div>
</template>

<style scoped>
.login-view {
  max-width: 400px;
  margin: 5rem auto;
  padding: 2rem;
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

h2 {
  text-align: center;
  margin-bottom: 2rem;
  color: #333;
}

.login-form {
  display: flex;
  flex-direction: column;
}

.form-group {
  margin-bottom: 1.5rem;
}

.form-group.has-error input {
  border-color: #e50914;
  background-color: #fff8f8;
}

label {
  display: block;
  margin-bottom: 0.5rem;
  font-weight: 600;
  color: #555;
}

input {
  width: 100%;
  padding: 0.8rem 1rem;
  border: 1px solid #ccc;
  border-radius: 4px;
  font-size: 1rem;
  transition: border-color 0.2s;
}

input:focus {
  outline: none;
  border-color: #e50914;
  box-shadow: 0 0 0 2px rgba(229, 9, 20, 0.2);
}

.field-error {
  color: #e50914;
  font-size: 0.85rem;
  margin-top: 0.3rem;
  padding-left: 0.5rem;
}

.error-message {
  background-color: #ffebee;
  color: #c62828;
  padding: 0.8rem 1rem;
  border-radius: 4px;
  margin-bottom: 1rem;
  text-align: center;
}

.btn-submit {
  padding: 0.8rem 1.5rem;
  background-color: #e50914;
  color: white;
  border: none;
  border-radius: 4px;
  font-size: 1.1rem;
  font-weight: 600;
  cursor: pointer;
  transition: background-color 0.2s;
}

.btn-submit:hover:not(:disabled) {
  background-color: #f40612;
}

.btn-submit:disabled {
  background-color: #ccc;
  cursor: not-allowed;
}

.register-link {
  margin-top: 1.5rem;
  text-align: center;
  color: #555;
}

.register-link a {
  color: #e50914;
  font-weight: 600;
}

.register-link a:hover {
  text-decoration: underline;
}
</style>
