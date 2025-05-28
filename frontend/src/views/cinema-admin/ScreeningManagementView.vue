<template>
  <div class="cinema-admin-screenings-view">
    <div class="page-header">
      <h2>场次管理</h2>
      <el-button type="primary" @click="openApplyDialog()">
        <el-icon><Plus /></el-icon> 申请新场次
      </el-button>
    </div>

    <!-- Filters -->
    <el-card class="filter-card" shadow="never">
      <el-form
        :inline="true"
        :model="filters"
        @submit.prevent="handleFilterChange"
        class="filter-form"
      >
        <el-form-item label="电影">
          <el-select
            v-model="filters.movieId"
            placeholder="所有电影"
            clearable
            filterable
            @change="handleFilterChange"
            style="width: 200px"
          >
            <el-option
              v-for="movie in availableMovies"
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
            @change="handleFilterChange"
            style="width: 150px"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select
            v-model="filters.status"
            placeholder="所有状态"
            clearable
            @change="handleFilterChange"
            style="width: 160px"
          >
            <el-option label="待审批" value="PENDING_APPROVAL" />
            <el-option label="已批准" value="APPROVED" />
            <el-option label="已拒绝" value="REJECTED" />
            <el-option label="已结束" value="FINISHED" />
            <el-option label="已取消" value="CANCELLED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleFilterChange">
            <el-icon><Search /></el-icon> 查询
          </el-button>
          <el-button @click="handleResetFilter">
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
        <el-table-column prop="id" label="ID" width="80" align="center" />
        <el-table-column prop="movieTitle" label="电影名称" min-width="150" show-overflow-tooltip />
        <el-table-column prop="roomName" label="影厅名称" min-width="120" />
        <el-table-column prop="startTime" label="开始时间" width="180" align="center">
          <template #default="{ row }">
            {{ formatDateTime(row.startTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="endTime" label="结束时间" width="180" align="center">
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
        <el-table-column label="操作" width="120" fixed="right" align="center">
          <template #default="{ row }">
            <el-button
              link
              type="danger"
              size="small"
              @click="handleCancelScreening(row)"
              :disabled="!canCancel(row)"
              v-if="canCancel(row)"
            >
              取消
            </el-button>
            <span v-else>-</span>
          </template>
        </el-table-column>
      </el-table>

      <!-- Pagination -->
      <el-pagination
        v-if="pagination.totalItems > 0"
        background
        layout="total, sizes, prev, pager, next, jumper"
        :total="pagination.totalItems"
        :current-page="pagination.currentPage"
        :page-size="pagination.pageSize"
        :page-sizes="[10, 20, 50]"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
        class="pagination-container"
      />
    </el-card>

    <!-- Apply Screening Dialog -->
    <el-dialog
      v-model="applyDialogVisible"
      title="申请新场次"
      width="600px"
      :close-on-click-modal="false"
      center
    >
      <el-form
        ref="applyFormRef"
        :model="newScreening"
        :rules="applyFormRules"
        label-width="100px"
        @submit.prevent="submitApplyForm"
      >
        <el-form-item label="选择电影" prop="movieId">
          <el-select
            v-model="newScreening.movieId"
            placeholder="请选择电影"
            filterable
            style="width: 100%"
          >
            <el-option
              v-for="movie in availableMovies"
              :key="movie.id"
              :label="movie.title"
              :value="movie.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="选择影厅" prop="roomId">
          <el-select v-model="newScreening.roomId" placeholder="请选择影厅" style="width: 100%">
            <el-option
              v-for="room in availableRooms"
              :key="room.id"
              :label="room.name"
              :value="room.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="开始时间" prop="startTime">
          <el-date-picker
            v-model="newScreening.startTime"
            type="datetime"
            placeholder="选择开始放映时间"
            value-format="YYYY-MM-DDTHH:mm:ss"
            format="YYYY-MM-DD HH:mm:ss"
            :default-time="defaultTimeForPicker"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="票价" prop="price">
          <el-input-number
            v-model="newScreening.price"
            :precision="2"
            :step="1"
            :min="0"
            placeholder="输入票价"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="applyDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitApplyForm" :loading="applying"
            >提交申请</el-button
          >
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import {
  ElTable,
  ElTableColumn,
  ElTag,
  ElButton,
  ElCard,
  ElForm,
  ElFormItem,
  ElSelect,
  ElOption,
  ElDatePicker,
  ElPagination,
  ElDialog,
  ElInputNumber,
  ElMessage,
  ElMessageBox,
} from 'element-plus'
import type { FormInstance, FormRules, TagProps } from 'element-plus'
import {
  listMyScreenings,
  fetchMovies,
  listMyRooms,
  applyForScreening,
  cancelScreening,
} from '@/services/api'
import { formatDateTime } from '@/utils/datetime'
import { Search, Refresh, Plus } from '@element-plus/icons-vue'
import type { Screening } from '@/types/screening'
import type { Movie } from '@/types/movie'
import type { Room } from '@/types/room'

// Define a type for Axios errors to safely access response data
interface AxiosErrorWithResponse {
  response?: {
    data?: {
      message?: string
      // Add other potential properties if needed
    }
    // Add other potential response properties if needed
  }
  // Add other potential error properties if needed
}

// Type for API parameters (matching api.ts definition)
interface ListMyScreeningsParamsFromAPI {
  current?: number
  size?: number
  movieId?: number
  date?: string
  status?: 'PENDING_APPROVAL' | 'APPROVED' | 'REJECTED' | 'FINISHED' | 'CANCELLED'
}

// Type for local filters (allowing string/empty for UI components)
interface LocalFilters {
  movieId: number | '' | undefined
  date: string | undefined
  status: 'PENDING_APPROVAL' | 'APPROVED' | 'REJECTED' | 'FINISHED' | 'CANCELLED' | ''
}

// --- State ---
const screenings = ref<Screening[]>([])
const availableMovies = ref<Movie[]>([])
const availableRooms = ref<Room[]>([])
const loading = ref(true)
const error = ref<string | null>(null)

const pagination = reactive({
  currentPage: 1,
  pageSize: 10,
  totalItems: 0,
})

// Use LocalFilters type for the reactive filters object
const filters = reactive<LocalFilters>({
  movieId: '',
  date: undefined,
  status: '',
})

const applyDialogVisible = ref(false)
const applying = ref(false)
const applyFormRef = ref<FormInstance>()
const newScreening = reactive({
  movieId: undefined as number | undefined,
  roomId: undefined as number | undefined,
  startTime: '',
  price: 0,
})
// Default time for ElDatePicker (e.g., 12:00:00)
const defaultTimeForPicker = new Date(2000, 1, 1, 12, 0, 0)

// --- Data Fetching ---
const fetchScreenings = async () => {
  loading.value = true
  error.value = null
  try {
    const params: ListMyScreeningsParamsFromAPI = {
      current: pagination.currentPage,
      size: pagination.pageSize,
      movieId: filters.movieId ? Number(filters.movieId) : undefined,
      date: filters.date ?? undefined,
      status: filters.status || undefined,
    }
    const response = await listMyScreenings(params)
    const pageData = response.data
    screenings.value =
      pageData.records?.map((s) => ({
        ...s,
        movieTitle:
          availableMovies.value.find((m) => m.id === s.movieId)?.title ?? `Movie ID ${s.movieId}`,
        roomName:
          availableRooms.value.find((r) => r.id === s.roomId)?.name ?? `Room ID ${s.roomId}`,
      })) || []
    pagination.totalItems = pageData.total || 0
  } catch (err) {
    console.error('Failed to fetch screenings:', err)
    error.value = '加载场次列表失败'
    screenings.value = []
    pagination.totalItems = 0
  } finally {
    loading.value = false
  }
}

const fetchMoviesForSelect = async () => {
  try {
    const response = await fetchMovies({ size: 500, status: 'NOW_PLAYING' })
    const responseComing = await fetchMovies({ size: 500, status: 'COMING_SOON' })
    availableMovies.value = [
      ...(response.data.records || []),
      ...(responseComing.data.records || []),
    ].sort((a, b) => (a.title || '').localeCompare(b.title || '', 'zh-CN'))
    if (!loading.value && screenings.value.length > 0) {
      fetchScreenings()
    }
  } catch (err) {
    console.error('Failed to fetch movies for select:', err)
    ElMessage.error('加载电影列表失败')
  }
}

const fetchRoomsForSelect = async () => {
  try {
    const response = await listMyRooms()
    availableRooms.value = response.data || []
    if (!loading.value && screenings.value.length > 0) {
      fetchScreenings()
    }
  } catch (err) {
    console.error('Failed to fetch rooms for select:', err)
    ElMessage.error('加载影厅列表失败')
  }
}

// --- Event Handlers ---
const handleFilterChange = () => {
  pagination.currentPage = 1
  fetchScreenings()
}

const handleSizeChange = (newSize: number) => {
  pagination.pageSize = newSize
  fetchScreenings()
}

const handleCurrentChange = (newPage: number) => {
  pagination.currentPage = newPage
  fetchScreenings()
}

const openApplyDialog = () => {
  resetApplyForm()
  applyDialogVisible.value = true
}

const resetApplyForm = () => {
  newScreening.movieId = undefined
  newScreening.roomId = undefined
  newScreening.startTime = ''
  newScreening.price = 0
  applyFormRef.value?.resetFields()
}

// Helper function for type guarding Axios errors
function isAxiosErrorWithResponse(error: unknown): error is AxiosErrorWithResponse {
  return typeof error === 'object' && error !== null && 'response' in error
}

const submitApplyForm = async () => {
  if (!applyFormRef.value) return
  await applyFormRef.value.validate(async (valid) => {
    if (valid) {
      applying.value = true
      try {
        const dataToSend = {
          movieId: newScreening.movieId!,
          roomId: newScreening.roomId!,
          startTime: newScreening.startTime,
          price: newScreening.price,
        }
        await applyForScreening(dataToSend)
        ElMessage.success('场次申请成功，等待审批')
        applyDialogVisible.value = false
        fetchScreenings()
      } catch (err: unknown) {
        console.error('Failed to apply for screening:', err)
        let message = '场次申请失败'
        if (isAxiosErrorWithResponse(err)) {
          message = err.response?.data?.message || message
        }
        ElMessage.error(message)
      } finally {
        applying.value = false
      }
    }
  })
}

const handleCancelScreening = async (screening: Screening) => {
  try {
    await ElMessageBox.confirm(`确定要取消场次 ID: ${screening.id} 吗?`, '取消确认', {
      confirmButtonText: '确定取消',
      cancelButtonText: '关闭',
      type: 'warning',
    })
    loading.value = true
    await cancelScreening(screening.id)
    ElMessage.success('场次取消成功')
    fetchScreenings()
  } catch (action: unknown) {
    if (action === 'cancel') {
      ElMessage.info('取消操作已关闭')
      loading.value = false
    } else {
      console.error('Failed to cancel screening:', action)
      let message = '取消场次失败'
      if (isAxiosErrorWithResponse(action)) {
        message = action.response?.data?.message || message
      }
      ElMessage.error(message)
      loading.value = false
    }
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
  switch (status) {
    case 'PENDING_APPROVAL':
      return '待审批'
    case 'APPROVED':
      return '已批准'
    case 'REJECTED':
      return '已拒绝'
    case 'FINISHED':
      return '已结束'
    case 'CANCELLED':
      return '已取消'
    default:
      return status || '未知'
  }
}

// Determine if a screening can be cancelled
const canCancel = (screening: Screening): boolean => {
  if (!screening || !screening.status || !screening.startTime) {
    return false
  }
  if (screening.status === 'PENDING_APPROVAL') {
    return true
  }
  if (screening.status === 'APPROVED') {
    try {
      return new Date(screening.startTime).getTime() > Date.now()
    } catch {
      return false
    }
  }
  return false
}

// Validation rules for the apply form
const applyFormRules = reactive<FormRules>({
  movieId: [{ required: true, message: '请选择电影', trigger: 'change', type: 'number' }],
  roomId: [{ required: true, message: '请选择影厅', trigger: 'change', type: 'number' }],
  startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  price: [
    { required: true, message: '请输入票价', trigger: ['blur', 'change'] },
    { type: 'number', min: 0, message: '票价不能为负数', trigger: ['blur', 'change'] },
  ],
})

// 添加重置筛选方法
const handleResetFilter = () => {
  filters.movieId = ''
  filters.date = undefined
  filters.status = ''
  handleFilterChange()
}

// --- Lifecycle Hooks ---
onMounted(() => {
  Promise.all([fetchMoviesForSelect(), fetchRoomsForSelect()]).then(() => {
    fetchScreenings()
  })
})
</script>

<style scoped>
.cinema-admin-screenings-view {
  padding: 20px;
  height: auto;
  width: 100%;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
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
  padding: 0;
  overflow: visible;
}

.filter-form .el-form-item {
  margin-bottom: 10px;
  margin-right: 10px;
}

.table-card {
  margin-bottom: 20px;
  overflow: visible;
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

:deep(.el-card__body) {
  padding: 15px;
  overflow: visible !important;
}

:deep(.el-table) {
  width: 100% !important;
  overflow: visible;
}

:deep(.el-table__body-wrapper) {
  overflow: visible;
}
</style>
