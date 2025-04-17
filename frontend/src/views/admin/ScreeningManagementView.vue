<template>
  <div class="screening-management-view">
    <div class="page-header">
      <h2>场次管理</h2>
    </div>

    <!-- Filters -->
    <el-card class="filter-card" shadow="never">
      <el-form :inline="true" :model="filters" @submit.prevent="handleFilter">
        <el-form-item label="影院">
          <el-select
            v-model="filters.cinemaId"
            placeholder="所有影院"
            clearable
            filterable
            @change="handleFilter"
            style="width: 200px"
            :loading="cinemaLoading"
          >
            <el-option
              v-for="cinema in cinemaOptions"
              :key="cinema.id"
              :label="cinema.name"
              :value="cinema.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="电影">
          <el-select
            v-model="filters.movieId"
            placeholder="所有电影"
            clearable
            filterable
            @change="handleFilter"
            style="width: 200px"
            :loading="movieLoading"
          >
            <el-option
              v-for="movie in movieOptions"
              :key="movie.id"
              :label="movie.title"
              :value="movie.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="日期">
          <el-date-picker
            v-model="filters.date"
            type="date"
            placeholder="选择日期"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            clearable
            @change="handleFilter"
            style="width: 180px"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select
            v-model="filters.status"
            placeholder="所有状态"
            clearable
            @change="handleFilter"
            style="width: 160px"
          >
            <el-option
              v-for="option in screeningStatusOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleFilter" :loading="loading">
            <el-icon><Search /></el-icon> 查询
          </el-button>
          <el-button @click="handleResetFilters">
            <el-icon><Refresh /></el-icon> 重置
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- Screenings Table -->
    <el-card class="table-card" shadow="never">
      <div class="table-header">
        <div class="table-title">场次列表</div>
      </div>

      <el-table :data="screenings" v-loading="loading" stripe border style="width: 100%">
        <el-table-column prop="screeningId" label="ID" width="80" align="center" />
        <el-table-column prop="cinemaName" label="影院名称" min-width="150" show-overflow-tooltip />
        <el-table-column prop="movieTitle" label="电影标题" min-width="150" show-overflow-tooltip />
        <el-table-column prop="roomName" label="影厅名称" width="120" align="center" />
        <el-table-column prop="startTime" label="开始时间" width="170" align="center">
          <template #default="{ row }">
            {{ formatDateTime(row.startTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="endTime" label="结束时间" width="170" align="center">
          <template #default="{ row }">
            {{ formatDateTime(row.endTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="price" label="价格" width="100" align="right">
          <template #default="{ row }"> ¥{{ row.price?.toFixed(2) ?? '?.??' }} </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusTagType(row.status)">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right" align="center">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 'PENDING_APPROVAL'"
              size="small"
              type="warning"
              @click="handleReview(row)"
            >
              审核
            </el-button>
            <span v-else>-</span>
          </template>
        </el-table-column>
      </el-table>

      <!-- Pagination -->
      <el-pagination
        v-if="pagination.total > 0"
        background
        layout="total, sizes, prev, pager, next, jumper"
        :total="pagination.total"
        v-model:current-page="pagination.currentPage"
        v-model:page-size="pagination.pageSize"
        :page-sizes="[10, 20, 50, 100]"
        @size-change="fetchScreenings"
        @current-change="fetchScreenings"
        class="pagination-container"
      />
    </el-card>

    <!-- Review Dialog -->
    <el-dialog
      v-model="reviewDialogVisible"
      title="审核场次申请"
      width="600px"
      :close-on-click-modal="false"
      center
    >
      <div v-if="selectedScreening" v-loading="reviewLoading">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="场次ID">{{
            selectedScreening.screeningId
          }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getStatusTagType(selectedScreening.status)">
              {{ getStatusText(selectedScreening.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="影院">{{
            selectedScreening.cinemaName
          }}</el-descriptions-item>
          <el-descriptions-item label="影厅">{{ selectedScreening.roomName }}</el-descriptions-item>
          <el-descriptions-item label="电影">{{
            selectedScreening.movieTitle
          }}</el-descriptions-item>
          <el-descriptions-item label="价格"
            >¥{{ selectedScreening.price?.toFixed(2) ?? '?.??' }}</el-descriptions-item
          >
          <el-descriptions-item label="开始时间">{{
            formatDateTime(selectedScreening.startTime)
          }}</el-descriptions-item>
          <el-descriptions-item label="结束时间">{{
            formatDateTime(selectedScreening.endTime)
          }}</el-descriptions-item>
        </el-descriptions>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="reviewDialogVisible = false" :disabled="reviewLoading">取消</el-button>
          <el-button type="danger" @click="submitReview(false)" :loading="reviewLoading"
            >拒绝</el-button
          >
          <el-button type="success" @click="submitReview(true)" :loading="reviewLoading"
            >批准</el-button
          >
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import {
  ElCard,
  ElForm,
  ElFormItem,
  ElSelect,
  ElOption,
  ElDatePicker,
  ElButton,
  ElTable,
  ElTableColumn,
  ElPagination,
  ElDialog,
  ElTag,
  ElMessage,
  ElDescriptions,
  ElDescriptionsItem,
  type TagProps,
} from 'element-plus'
import {
  listAdminScreenings,
  reviewScreening,
  listAdminCinemas,
  fetchMovies, // Assuming fetchMovies lists movies
  type ListAdminScreeningsParams,
  type ScreeningReviewRequest,
} from '@/services/api' // Adjust path if needed
import type { ScreeningDetailVO } from '@/types/screening' // Adjust path
import type { Cinema } from '@/types/cinema' // Adjust path
import type { Movie } from '@/types/movie' // Adjust path
import type { Page } from '@/types/page' // Adjust path
import { formatDateTime } from '@/utils/datetime' // Adjust path
import { Search, Refresh } from '@element-plus/icons-vue'

// --- Component State ---
const loading = ref(false)
const cinemaLoading = ref(false)
const movieLoading = ref(false)
const screenings = ref<ScreeningDetailVO[]>([]) // Use ScreeningDetailVO from API response
const pagination = reactive({
  currentPage: 1,
  pageSize: 10,
  total: 0,
})
const filters = reactive<{
  cinemaId: number | '' | undefined
  movieId: number | '' | undefined
  date: string | undefined
  status: ListAdminScreeningsParams['status'] | ''
}>({
  cinemaId: '',
  movieId: '',
  date: undefined,
  status: '',
})
const screeningStatusOptions = [
  { value: 'PENDING_APPROVAL', label: '待审批' },
  { value: 'APPROVED', label: '已批准' },
  { value: 'REJECTED', label: '已拒绝' },
  { value: 'FINISHED', label: '已结束' },
  { value: 'CANCELLED', label: '已取消' },
]
const cinemaOptions = ref<Cinema[]>([])
const movieOptions = ref<Movie[]>([])

const reviewDialogVisible = ref(false)
const reviewLoading = ref(false)
const selectedScreening = ref<ScreeningDetailVO | null>(null)

// --- Data Fetching Logic ---
const fetchScreenings = async () => {
  loading.value = true
  try {
    const params: ListAdminScreeningsParams = {
      current: pagination.currentPage,
      size: pagination.pageSize,
      cinemaId: filters.cinemaId ? Number(filters.cinemaId) : undefined,
      movieId: filters.movieId ? Number(filters.movieId) : undefined,
      date: filters.date || undefined,
      status: filters.status || undefined,
    }
    const response = await listAdminScreenings(params)
    const pageData: Page<ScreeningDetailVO> = response.data
    screenings.value = pageData.records || []
    pagination.total = pageData.total || 0
  } catch (error) {
    console.error('获取场次列表失败:', error)
    ElMessage.error('获取场次列表失败')
    screenings.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

const fetchCinemasForFilter = async () => {
  cinemaLoading.value = true
  try {
    // Fetch a reasonable number of cinemas for the filter
    const response = await listAdminCinemas({ size: 500 }) // Adjust size as needed
    cinemaOptions.value = response.data.records || []
  } catch (error) {
    console.error('获取影院筛选列表失败:', error)
    // Non-critical error, filter might just be empty
    ElMessage.warning('无法加载影院筛选列表')
    cinemaOptions.value = []
  } finally {
    cinemaLoading.value = false
  }
}

const fetchMoviesForFilter = async () => {
  movieLoading.value = true
  try {
    // Fetch a reasonable number of movies (e.g., Now Playing + Coming Soon)
    const params = { size: 500 } // Adjust size
    const responseNowPlaying = await fetchMovies({ ...params, status: 'NOW_PLAYING' })
    const responseComingSoon = await fetchMovies({ ...params, status: 'COMING_SOON' })
    movieOptions.value = [
      ...(responseNowPlaying.data.records || []),
      ...(responseComingSoon.data.records || []),
    ].sort((a, b) => (a.title || '').localeCompare(b.title || '', 'zh-CN'))
  } catch (error) {
    console.error('获取电影筛选列表失败:', error)
    ElMessage.warning('无法加载电影筛选列表')
    movieOptions.value = []
  } finally {
    movieLoading.value = false
  }
}

// --- Event Handlers ---
const handleFilter = () => {
  pagination.currentPage = 1
  fetchScreenings()
}

const handleResetFilters = () => {
  filters.cinemaId = ''
  filters.movieId = ''
  filters.date = undefined
  filters.status = ''
  handleFilter()
}

const handleReview = (row: ScreeningDetailVO) => {
  selectedScreening.value = row
  reviewDialogVisible.value = true
}

// --- Form Submissions ---
const submitReview = async (approved: boolean) => {
  if (!selectedScreening.value?.screeningId) return

  reviewLoading.value = true
  try {
    const payload: ScreeningReviewRequest = { approved }
    await reviewScreening(selectedScreening.value.screeningId, payload)
    ElMessage.success(`场次已${approved ? '批准' : '拒绝'}`)
    reviewDialogVisible.value = false
    fetchScreenings() // Refresh list to show updated status
  } catch (error: unknown) {
    console.error('审核场次失败:', error)
    let message = '审核失败'
    if (error instanceof Error) {
      message = error.message
    }
    if (typeof error === 'object' && error !== null && 'response' in error) {
      const responseError = error as { response?: { data?: { message?: string } } }
      message = responseError.response?.data?.message || message
    }
    ElMessage.error(message)
  } finally {
    reviewLoading.value = false
  }
}

// --- Computed & Helpers ---
const getStatusTagType = (status: string | undefined): TagProps['type'] => {
  switch (status) {
    case 'PENDING_APPROVAL':
      return 'warning'
    case 'APPROVED':
      return 'success'
    case 'REJECTED':
      return 'danger'
    case 'FINISHED':
      return 'info'
    case 'CANCELLED':
      return 'info'
    default:
      return 'info'
  }
}

const getStatusText = (status: string | undefined): string => {
  const option = screeningStatusOptions.find((o) => o.value === status)
  return option ? option.label : status || '未知'
}

// --- Lifecycle Hooks ---
onMounted(() => {
  fetchScreenings()
  fetchCinemasForFilter()
  fetchMoviesForFilter()
})
</script>

<style scoped>
.screening-management-view {
  padding: 20px;
}

.page-header {
  margin-bottom: 20px;
}

.page-header h2 {
  font-size: 22px;
  font-weight: bold;
  margin: 0;
  color: #303133;
}

.filter-card {
  margin-bottom: 20px;
  padding: 10px 20px;
}

.filter-card .el-form-item {
  margin-bottom: 10px;
}

.table-card {
  margin-bottom: 20px;
}

.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.table-title {
  font-size: 16px;
  font-weight: bold;
  color: #303133;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.dialog-footer {
  width: 100%;
  display: flex;
  justify-content: flex-end;
}
</style>
