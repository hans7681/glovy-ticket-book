<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { register as apiRegister } from '@/services/api' // Use relative path if needed
import type { RegisterPayload, ErrorResponse } from '@/types/auth' // Use relative path if needed
import type { AxiosError } from 'axios'

const router = useRouter()

const username = ref('')
const password = ref('')
const confirmPassword = ref('')
const nickname = ref('')
const phone = ref('')
const email = ref('')

const fieldErrors = ref<Record<string, string>>({})
const errorMessage = ref<string | null>(null)
const successMessage = ref<string | null>(null)
const isLoading = ref(false)

const validateForm = () => {
  fieldErrors.value = {}
  errorMessage.value = null

  let isValid = true

  if (!username.value.trim()) {
    fieldErrors.value.username = '用户名不能为空'
    isValid = false
  } else if (username.value.length < 3 || username.value.length > 20) {
    fieldErrors.value.username = '用户名长度应在3-20个字符之间'
    isValid = false
  }

  if (!password.value) {
    fieldErrors.value.password = '密码不能为空'
    isValid = false
  } else if (password.value.length < 6) {
    fieldErrors.value.password = '密码长度至少需要6位'
    isValid = false
  }

  if (password.value !== confirmPassword.value) {
    fieldErrors.value.confirmPassword = '两次输入的密码不一致'
    isValid = false
  }

  if (email.value && !/^\S+@\S+\.\S+$/.test(email.value)) {
    fieldErrors.value.email = '请输入有效的邮箱地址'
    isValid = false
  }

  if (phone.value && !/^1[3-9]\d{9}$/.test(phone.value)) {
    fieldErrors.value.phone = '请输入有效的11位手机号码'
    isValid = false
  }

  return isValid
}

const handleRegister = async () => {
  if (!validateForm()) {
    return
  }

  isLoading.value = true
  const payload: RegisterPayload = {
    username: username.value,
    password: password.value,
    nickname: nickname.value || undefined,
    phone: phone.value || undefined,
    email: email.value || undefined,
    role: 'USER',
  }

  try {
    const response = await apiRegister(payload)
    if (response.status === 201 && response.data) {
      successMessage.value = `注册成功！用户 ${response.data.username} 已创建。即将跳转到登录页面...`
      setTimeout(() => {
        router.push('/login')
      }, 3000)
    } else {
      errorMessage.value = '注册请求失败，请稍后重试。'
    }
  } catch (error) {
    console.error('Registration failed:', error)
    const axiosError = error as AxiosError<ErrorResponse>

    if (axiosError.response?.data) {
      const errorData = axiosError.response.data

      if (errorData.fieldErrors && errorData.fieldErrors.length > 0) {
        errorData.fieldErrors.forEach((fieldError) => {
          fieldErrors.value[fieldError.field] = fieldError.message
        })
        errorMessage.value = '请修正表单中的错误后重试'
      } else {
        errorMessage.value = errorData.message || '注册失败，请稍后重试'
      }
    } else if (axiosError.response?.status === 409) {
      errorMessage.value = '用户名、邮箱或手机号已被使用'
    } else {
      errorMessage.value = '注册失败，请稍后重试'
    }
  } finally {
    isLoading.value = false
  }
}
</script>

<template>
  <div class="register-view">
    <h2>用户注册</h2>
    <form @submit.prevent="handleRegister" class="register-form">
      <div class="form-group" :class="{ 'has-error': fieldErrors.username }">
        <label for="username">用户名 <span class="required">*</span></label>
        <input
          type="text"
          id="username"
          v-model="username"
          required
          placeholder="请输入用户名（3-20个字符）"
        />
        <div class="field-error" v-if="fieldErrors.username">{{ fieldErrors.username }}</div>
      </div>
      <div class="form-group" :class="{ 'has-error': fieldErrors.password }">
        <label for="password">密码 <span class="required">*</span></label>
        <input
          type="password"
          id="password"
          v-model="password"
          required
          placeholder="请输入至少6位密码"
        />
        <div class="field-error" v-if="fieldErrors.password">{{ fieldErrors.password }}</div>
      </div>
      <div class="form-group" :class="{ 'has-error': fieldErrors.confirmPassword }">
        <label for="confirmPassword">确认密码 <span class="required">*</span></label>
        <input
          type="password"
          id="confirmPassword"
          v-model="confirmPassword"
          required
          placeholder="请再次输入密码"
        />
        <div class="field-error" v-if="fieldErrors.confirmPassword">
          {{ fieldErrors.confirmPassword }}
        </div>
      </div>
      <div class="form-group" :class="{ 'has-error': fieldErrors.nickname }">
        <label for="nickname">昵称</label>
        <input type="text" id="nickname" v-model="nickname" placeholder="请输入昵称" />
        <div class="field-error" v-if="fieldErrors.nickname">{{ fieldErrors.nickname }}</div>
      </div>
      <div class="form-group" :class="{ 'has-error': fieldErrors.phone }">
        <label for="phone">手机号</label>
        <input type="tel" id="phone" v-model="phone" placeholder="请输入11位手机号码" />
        <div class="field-error" v-if="fieldErrors.phone">{{ fieldErrors.phone }}</div>
      </div>
      <div class="form-group" :class="{ 'has-error': fieldErrors.email }">
        <label for="email">邮箱</label>
        <input type="email" id="email" v-model="email" placeholder="请输入邮箱" />
        <div class="field-error" v-if="fieldErrors.email">{{ fieldErrors.email }}</div>
      </div>

      <div v-if="errorMessage" class="error-message">
        {{ errorMessage }}
      </div>
      <div v-if="successMessage" class="success-message">
        {{ successMessage }}
      </div>

      <button type="submit" class="btn-submit" :disabled="isLoading">
        {{ isLoading ? '注册中...' : '注册' }}
      </button>
      <p class="login-link">已有账号？ <RouterLink to="/login">立即登录</RouterLink></p>
    </form>
  </div>
</template>

<style scoped>
.register-view {
  max-width: 450px;
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

.register-form {
  display: flex;
  flex-direction: column;
}

.form-group {
  margin-bottom: 1rem;
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

.required {
  color: #e50914;
  margin-left: 0.2rem;
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

.error-message,
.success-message {
  padding: 0.8rem 1rem;
  border-radius: 4px;
  margin-bottom: 1rem;
  text-align: center;
}

.error-message {
  background-color: #ffebee;
  color: #c62828;
}

.success-message {
  background-color: #e8f5e9;
  color: #2e7d32;
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
  margin-top: 1rem;
}

.btn-submit:hover:not(:disabled) {
  background-color: #f40612;
}

.btn-submit:disabled {
  background-color: #ccc;
  cursor: not-allowed;
}

.login-link {
  margin-top: 1.5rem;
  text-align: center;
  color: #555;
}

.login-link a {
  color: #e50914;
  font-weight: 600;
}

.login-link a:hover {
  text-decoration: underline;
}
</style>
