<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { RouterLink } from 'vue-router'
import { fetchApprovedCinemas } from '../services/api'
import type { Cinema } from '../types/cinema'
import {
  ElMessage,
  ElCard,
  ElForm,
  ElFormItem,
  ElInput,
  ElSelect,
  ElOption,
  ElButton,
  ElPagination,
  ElEmpty,
} from 'element-plus'

const cinemas = ref<Cinema[]>([])
const isLoading = ref(true)
const error = ref<string | null>(null)

// 修改分页为对象形式
const pagination = ref({
  current: 1,
  size: 10,
  total: 0,
  pages: 1,
})

// 添加筛选条件
const searchName = ref('')
const searchLocation = ref('')
const selectedFeature = ref('')

// 特色功能选项
const featureOptions = [
  { label: '全部特色', value: '' },
  { label: 'IMAX厅', value: 'IMAX' },
  { label: '杜比全景声', value: 'Dolby Atmos' },
  { label: '4D厅', value: '4D' },
  { label: '3D厅', value: '3D' },
]

// Function to construct full image URL for logo
const getLogoUrl = (path: string | undefined) => {
  if (!path) {
    // Return a default placeholder or leave empty based on design
    return 'https://via.placeholder.com/100x50.png?text=No+Logo'
  }
  // Assuming path is a full URL
  return path
}

// 处理获取影院列表的方法，添加筛选和分页功能
async function loadCinemas(resetPage: boolean = false) {
  if (resetPage) {
    pagination.value.current = 1 // 重置到第一页
  }

  isLoading.value = true
  error.value = null

  try {
    const response = await fetchApprovedCinemas({
      current: pagination.value.current,
      size: pagination.value.size,
      name: searchName.value || undefined,
      location: searchLocation.value || undefined,
      feature: selectedFeature.value || undefined,
    })

    const pageData = response.data
    cinemas.value = pageData.records
    pagination.value.current = pageData.current
    pagination.value.pages = pageData.pages
    pagination.value.total = pageData.total
  } catch (err) {
    console.error('Failed to load cinemas:', err)
    error.value = '无法加载影院列表，请稍后重试。'
    cinemas.value = []
    pagination.value.current = 1
    pagination.value.pages = 1
    pagination.value.total = 0
    ElMessage.error('加载影院列表失败')
  } finally {
    isLoading.value = false
  }
}

// 处理搜索按钮点击
const handleSearch = () => {
  loadCinemas(true) // 触发搜索并重置页码
}

// 处理分页变化
const handlePageChange = (newPage: number) => {
  pagination.value.current = newPage
  loadCinemas()
}

// 处理每页条数变化
const handleSizeChange = (newSize: number) => {
  pagination.value.size = newSize
  loadCinemas(true) // 改变每页数量时也重置到第一页
}

// Load cinemas on mount
onMounted(() => {
  loadCinemas()
})

// Watch for page changes (保留兼容性)
watch(
  () => pagination.value.current,
  (newPage) => {
    if (newPage) {
      loadCinemas()
    }
  },
)
</script>

<template>
  <div class="cinema-list-view">
    <h1 class="page-title">影院列表</h1>

    <!-- 筛选区域 -->
    <el-card shadow="never" class="filter-card">
      <el-form :inline="true" @submit.prevent="handleSearch">
        <el-form-item label="影院名称">
          <el-input
            v-model="searchName"
            placeholder="输入影院名称"
            clearable
            style="width: 180px"
          />
        </el-form-item>
        <el-form-item label="地区/城市">
          <el-input
            v-model="searchLocation"
            placeholder="输入地区或城市"
            clearable
            style="width: 180px"
          />
        </el-form-item>
        <el-form-item label="特色功能">
          <el-select
            v-model="selectedFeature"
            placeholder="全部功能"
            clearable
            style="width: 160px"
            @change="handleSearch"
          >
            <el-option
              v-for="item in featureOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch" :loading="isLoading">查询</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- Loading State -->
    <div v-if="isLoading" class="loading-container">
      <p>正在加载影院列表...</p>
      <!-- Optional: Add spinner -->
      <div class="cinema-list-loading">
        <div class="cinema-card-placeholder" v-for="i in pagination.size" :key="`ph-${i}`">
          <div class="logo-placeholder placeholder"></div>
          <div class="info-placeholder">
            <div class="name-placeholder placeholder-text"></div>
            <div class="address-placeholder placeholder-text"></div>
          </div>
        </div>
      </div>
    </div>

    <!-- Error State -->
    <div v-else-if="error" class="error-message">
      {{ error }}
    </div>

    <!-- Success State -->
    <div v-else>
      <ul v-if="cinemas.length > 0" class="cinema-list">
        <li v-for="cinema in cinemas" :key="cinema.id" class="cinema-item">
          <RouterLink :to="'/cinemas/' + cinema.id" class="cinema-link">
            <img :src="getLogoUrl(cinema.logo)" :alt="cinema.name + ' Logo'" class="cinema-logo" />
            <div class="cinema-info">
              <h2 class="cinema-name">{{ cinema.name }}</h2>
              <p class="cinema-address">{{ cinema.address }}</p>
              <p v-if="cinema.phone" class="cinema-phone">电话: {{ cinema.phone }}</p>
            </div>
          </RouterLink>
        </li>
      </ul>
      <el-empty v-else description="未找到符合条件的影院"></el-empty>

      <!-- 改用 Element Plus 分页组件 -->
      <el-pagination
        v-if="pagination.total > 0"
        background
        layout="prev, pager, next, jumper, ->, total, sizes"
        :total="pagination.total"
        v-model:current-page="pagination.current"
        v-model:page-size="pagination.size"
        :page-sizes="[10, 20, 50]"
        @size-change="handleSizeChange"
        @current-change="handlePageChange"
        style="margin-top: 20px; justify-content: center"
        :hide-on-single-page="pagination.total <= pagination.size"
      />
    </div>
  </div>
</template>

<style scoped>
.cinema-list-view {
  padding: 2rem;
  max-width: 1000px;
  margin: 0 auto;
}

.page-title {
  font-size: 2.5rem;
  margin-bottom: 2rem;
  text-align: center;
  color: #333;
}

/* 筛选区域样式 */
.filter-card {
  margin-bottom: 1.5rem;
}

.filter-card :deep(.el-card__body) {
  padding: 15px;
}

/* 尝试减小 inline 表单项的右边距 */
.filter-card .el-form--inline .el-form-item {
  margin-right: 10px; /* 减小默认的右边距 */
}

.loading-container {
  text-align: center;
  padding: 2rem;
  color: #555;
}

.error-message {
  color: #c62828;
  background-color: #ffebee;
  padding: 1rem;
  border-radius: 4px;
  text-align: center;
  margin-top: 2rem;
}

.cinema-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.cinema-item {
  margin-bottom: 1.5rem;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  overflow: hidden;
  transition: box-shadow 0.3s;
}

.cinema-item:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.cinema-link {
  display: flex;
  padding: 1.5rem;
  text-decoration: none;
  color: inherit;
  background-color: #fff;
  gap: 1.5rem;
  align-items: center;
}

.cinema-logo {
  width: 120px;
  height: 60px; /* Adjust aspect ratio as needed */
  object-fit: contain; /* Use contain to avoid cropping logo */
  border-radius: 4px;
  flex-shrink: 0;
  border: 1px solid #eee;
}

.cinema-info {
  flex-grow: 1;
}

.cinema-name {
  font-size: 1.4rem;
  font-weight: 600;
  color: #333;
  margin: 0 0 0.5rem 0;
}

.cinema-address,
.cinema-phone {
  color: #555;
  font-size: 0.95rem;
  margin: 0.25rem 0;
  line-height: 1.4;
}

/* Placeholder Styles */
.cinema-list-loading {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
  margin-top: 1rem;
}
.cinema-card-placeholder {
  display: flex;
  gap: 1.5rem;
  padding: 1.5rem;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  background-color: #fff;
  align-items: center;
}

.placeholder {
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: loading 1.5s infinite;
}

.logo-placeholder {
  width: 120px;
  height: 60px;
  border-radius: 4px;
  flex-shrink: 0;
}

.info-placeholder {
  flex-grow: 1;
}

.placeholder-text {
  height: 16px;
  margin-bottom: 8px;
  border-radius: 2px;
}

.name-placeholder {
  width: 60%;
}

.address-placeholder {
  width: 80%;
}

@keyframes loading {
  0% {
    background-position: 200% 0;
  }
  100% {
    background-position: -200% 0;
  }
}

/* 响应式调整 */
@media (max-width: 768px) {
  .cinema-link {
    flex-direction: column;
    align-items: flex-start;
  }

  .cinema-logo {
    margin-bottom: 1rem;
  }
}
</style>
