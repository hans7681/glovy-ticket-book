<template>
  <div class="cinema-management-view">
    <div class="page-header">
      <h2>影院管理</h2>
    </div>

    <!-- Filters -->
    <el-card class="filter-card" shadow="never">
      <el-form :inline="true" :model="filters" @submit.prevent="handleFilter">
        <el-form-item label="影院状态">
          <el-select
            v-model="filters.status"
            placeholder="所有状态"
            clearable
            @change="handleFilter"
            style="width: 180px"
          >
            <el-option
              v-for="option in cinemaStatusOptions"
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

    <!-- Cinema Table -->
    <el-card class="table-card" shadow="never">
      <div class="table-header">
        <div class="table-title">影院列表</div>
      </div>

      <el-table :data="cinemas" v-loading="loading" stripe border style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" align="center" />
        <el-table-column prop="name" label="影院名称" min-width="150" show-overflow-tooltip />
        <el-table-column prop="address" label="地址" min-width="200" show-overflow-tooltip />
        <el-table-column prop="adminUserId" label="关联管理员ID" width="140" align="center">
          <template #default="{ row }">
            {{ row.adminUserId || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="140" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusTagType(row.status)">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="申请时间" width="170" align="center">
          <template #default="{ row }">
            {{ formatDateTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right" align="center">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click="handleEdit(row)">
              <el-icon><Edit /></el-icon> 编辑
            </el-button>
            <el-button
              v-if="row.status === 'PENDING_APPROVAL'"
              size="small"
              type="warning"
              @click="handleReview(row)"
            >
              审核
            </el-button>
            <el-button size="small" type="info" @click="handleStatusChange(row)"> 状态 </el-button>
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
        @size-change="fetchCinemas"
        @current-change="fetchCinemas"
        class="pagination-container"
      />
    </el-card>

    <!-- Edit Dialog -->
    <el-dialog
      v-model="editDialogVisible"
      title="编辑影院信息"
      width="600px"
      :close-on-click-modal="false"
      center
    >
      <el-form
        ref="editFormRef"
        :model="editForm"
        :rules="editFormRules"
        label-width="120px"
        label-position="right"
        v-loading="editLoading"
      >
        <el-form-item label="影院名称" prop="name">
          <el-input v-model="editForm.name" placeholder="输入影院名称" style="width: 100%" />
        </el-form-item>
        <el-form-item label="地址" prop="address">
          <el-input v-model="editForm.address" placeholder="输入影院地址" style="width: 100%" />
        </el-form-item>
        <el-form-item label="联系电话" prop="phone">
          <el-input v-model="editForm.phone" placeholder="输入联系电话" style="width: 100%" />
        </el-form-item>
        <el-form-item label="Logo URL" prop="logo">
          <el-input v-model="editForm.logo" placeholder="输入Logo图片URL" style="width: 100%" />
        </el-form-item>
        <el-form-item label="影院介绍" prop="description">
          <el-input
            type="textarea"
            v-model="editForm.description"
            :rows="3"
            placeholder="输入影院介绍"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="关联管理员" prop="adminUserId">
          <el-select
            v-model="editForm.adminUserId"
            placeholder="选择影院管理员"
            clearable
            filterable
            style="width: 100%"
            :loading="adminUserLoading"
          >
            <el-option
              v-for="user in cinemaAdminUsers"
              :key="user.id"
              :label="`${user.nickname || user.username} (ID: ${user.id})`"
              :value="user.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="当前状态" prop="status">
          <el-tag :type="getStatusTagType(editForm.status)">
            {{ getStatusText(editForm.status) }}
          </el-tag>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="editDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitEditForm" :loading="editLoading">保存</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- Review Dialog -->
    <el-dialog
      v-model="reviewDialogVisible"
      title="审核影院申请"
      width="600px"
      :close-on-click-modal="false"
      center
    >
      <div v-if="selectedCinema" v-loading="reviewLoading">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="影院ID">{{ selectedCinema.id }}</el-descriptions-item>
          <el-descriptions-item label="影院名称">{{ selectedCinema.name }}</el-descriptions-item>
          <el-descriptions-item label="地址">{{ selectedCinema.address }}</el-descriptions-item>
          <el-descriptions-item label="申请时间">{{
            formatDateTime(selectedCinema.createTime)
          }}</el-descriptions-item>
        </el-descriptions>
        <el-form
          ref="reviewFormRef"
          :model="reviewForm"
          :rules="reviewFormRules"
          label-width="120px"
          style="margin-top: 20px"
        >
          <el-form-item label="关联管理员" prop="adminUserId">
            <el-select
              v-model="reviewForm.adminUserId"
              placeholder="批准时必须选择管理员"
              clearable
              filterable
              style="width: 100%"
              :loading="adminUserLoading"
            >
              <el-option
                v-for="user in cinemaAdminUsers"
                :key="user.id"
                :label="`${user.nickname || user.username} (ID: ${user.id})`"
                :value="user.id"
              />
            </el-select>
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="reviewDialogVisible = false" :disabled="reviewLoading">取消</el-button>
        <el-button type="danger" @click="submitReview(false)" :loading="reviewLoading"
          >拒绝</el-button
        >
        <el-button type="success" @click="submitReview(true)" :loading="reviewLoading"
          >批准</el-button
        >
      </template>
    </el-dialog>

    <!-- Status Change Dialog -->
    <el-dialog
      v-model="statusDialogVisible"
      title="变更影院状态"
      width="500px"
      :close-on-click-modal="false"
      center
    >
      <div v-if="selectedCinema" v-loading="statusLoading">
        <p>
          正在为影院
          <strong>{{ selectedCinema.name }} (ID: {{ selectedCinema.id }})</strong> 变更状态。
        </p>
        <p>
          当前状态:
          <el-tag :type="getStatusTagType(selectedCinema.status)">{{
            getStatusText(selectedCinema.status)
          }}</el-tag>
        </p>
        <el-form
          ref="statusFormRef"
          :model="statusForm"
          :rules="statusFormRules"
          label-width="100px"
          style="margin-top: 20px"
        >
          <el-form-item label="选择新状态" prop="status">
            <el-select v-model="statusForm.status" placeholder="请选择新的状态" style="width: 100%">
              <el-option
                v-for="option in availableStatusOptions"
                :key="option.value"
                :label="option.label"
                :value="option.value"
              />
            </el-select>
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="statusDialogVisible = false" :disabled="statusLoading">取消</el-button>
        <el-button type="primary" @click="submitStatusChange" :loading="statusLoading"
          >确认变更</el-button
        >
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import {
  ElCard,
  ElForm,
  ElFormItem,
  ElSelect,
  ElOption,
  ElButton,
  ElTable,
  ElTableColumn,
  ElPagination,
  ElDialog,
  ElInput,
  ElTag,
  ElMessage,
  ElDescriptions,
  ElDescriptionsItem,
  type FormInstance,
  type FormRules,
  type TagProps,
} from 'element-plus'
import {
  listAdminCinemas,
  reviewCinema,
  updateAdminCinema,
  updateCinemaStatus,
  listAdminUsers,
  type ListAdminCinemasParams,
  type CinemaReviewRequest,
  type CinemaStatusUpdateRequest,
  type ListAdminUsersParams,
} from '@/services/api' // Adjust import path if needed
import type { Cinema } from '@/types/cinema' // Adjust import path if needed
import type { User } from '@/types/user' // Adjust import path if needed
import type { Page } from '@/types/page' // Adjust import path if needed
import { formatDateTime } from '@/utils/datetime' // Adjust import path if needed
import { Search, Refresh, Edit } from '@element-plus/icons-vue'

// --- Component State ---
const loading = ref(false)
const cinemas = ref<Cinema[]>([])
const pagination = reactive({
  currentPage: 1,
  pageSize: 10,
  total: 0,
})
const filters = reactive({
  status: '' as ListAdminCinemasParams['status'] | '', // Allow empty string for 'All'
})
const cinemaStatusOptions = [
  { value: 'PENDING_APPROVAL', label: '待审核' },
  { value: 'APPROVED', label: '已批准' },
  { value: 'REJECTED', label: '已拒绝' },
  { value: 'DISABLED', label: '已禁用' },
]

const editDialogVisible = ref(false)
const editLoading = ref(false)
const editFormRef = ref<FormInstance>()
const editForm = reactive<Partial<Cinema>>({}) // Use Partial<Cinema> for editing

const reviewDialogVisible = ref(false)
const reviewLoading = ref(false)
const reviewFormRef = ref<FormInstance>()
const reviewForm = reactive({ adminUserId: undefined as number | undefined }) // Only need adminUserId for review form

const statusDialogVisible = ref(false)
const statusLoading = ref(false)
const statusFormRef = ref<FormInstance>()
const statusForm = reactive({ status: '' as CinemaStatusUpdateRequest['status'] | '' })

const selectedCinema = ref<Cinema | null>(null)
const cinemaAdminUsers = ref<User[]>([])
const adminUserLoading = ref(false)

// --- Data Fetching Logic ---
const fetchCinemas = async () => {
  loading.value = true
  try {
    const params: ListAdminCinemasParams = {
      current: pagination.currentPage,
      size: pagination.pageSize,
      status: filters.status || undefined, // Convert empty string to undefined for API
    }
    const response = await listAdminCinemas(params)
    const pageData: Page<Cinema> = response.data
    cinemas.value = pageData.records || []
    pagination.total = pageData.total || 0
  } catch (error) {
    console.error('获取影院列表失败:', error)
    ElMessage.error('获取影院列表失败')
    cinemas.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

const fetchAdminUsers = async () => {
  adminUserLoading.value = true
  try {
    const params: ListAdminUsersParams = {
      role: 'CINEMA_ADMIN',
      size: 1000, // Fetch a large number to likely get all admins
    }
    const response = await listAdminUsers(params)
    cinemaAdminUsers.value = response.data.records || []
  } catch (error) {
    console.error('获取影院管理员列表失败:', error)
    ElMessage.error('获取影院管理员列表失败')
    cinemaAdminUsers.value = []
  } finally {
    adminUserLoading.value = false
  }
}

// --- Event Handlers ---
const handleFilter = () => {
  pagination.currentPage = 1
  fetchCinemas()
}

const handleResetFilters = () => {
  filters.status = ''
  handleFilter()
}

const handleEdit = async (row: Cinema) => {
  selectedCinema.value = row
  // Reset form and copy data
  Object.assign(editForm, {
    id: row.id,
    name: row.name,
    address: row.address,
    phone: row.phone,
    logo: row.logo,
    description: row.description,
    adminUserId:
      row.adminUserId === null || row.adminUserId === undefined
        ? undefined
        : Number(row.adminUserId),
    status: row.status, // Include status for display
  })
  editFormRef.value?.clearValidate()
  editDialogVisible.value = true
  // Fetch admin users if not already fetched
  if (cinemaAdminUsers.value.length === 0) {
    await fetchAdminUsers()
  }
}

const handleReview = async (row: Cinema) => {
  selectedCinema.value = row
  reviewForm.adminUserId = undefined // Reset admin user selection
  reviewFormRef.value?.clearValidate()
  reviewDialogVisible.value = true
  // Fetch admin users if not already fetched
  if (cinemaAdminUsers.value.length === 0) {
    await fetchAdminUsers()
  }
}

const handleStatusChange = (row: Cinema) => {
  selectedCinema.value = row
  statusForm.status = '' // Reset status selection
  statusFormRef.value?.clearValidate()
  statusDialogVisible.value = true
}

// --- Form Submissions ---
const submitEditForm = async () => {
  if (!editFormRef.value || !selectedCinema.value?.id) return
  await editFormRef.value.validate(async (valid) => {
    if (valid) {
      // --- 开始校验管理员关联 ---
      let finalAdminUserId: number | undefined = undefined
      if (editForm.adminUserId !== null && editForm.adminUserId !== undefined) {
        const parsedId = Number(editForm.adminUserId)
        if (!isNaN(parsedId)) {
          finalAdminUserId = parsedId
        } else {
          console.warn('adminUserId could not be parsed to a number:', editForm.adminUserId)
        }
      }

      if (finalAdminUserId !== undefined) {
        const isAlreadyAssigned = cinemas.value.some(
          (cinema) =>
            cinema.id !== selectedCinema.value!.id && // 排除当前编辑的影院
            cinema.adminUserId === finalAdminUserId,
        )
        if (isAlreadyAssigned) {
          ElMessage.error('你选择的用户已关联其它影院')
          return // 阻止提交
        }
      }
      // --- 结束校验管理员关联 ---

      editLoading.value = true
      try {
        const payload: Partial<Cinema> = {
          name: editForm.name,
          address: editForm.address,
          phone: editForm.phone,
          logo: editForm.logo,
          description: editForm.description,
          adminUserId: finalAdminUserId, // 使用校验过的 finalAdminUserId
        }

        await updateAdminCinema(selectedCinema.value!.id, payload)
        ElMessage.success('影院信息更新成功')
        editDialogVisible.value = false
        fetchCinemas() // Refresh list
      } catch (error: unknown) {
        // Specify type as unknown
        console.error('更新影院信息失败:', error)
        // Type checking for error message extraction
        let message = '未知错误'
        if (error instanceof Error) {
          message = error.message
        }
        // Example check if it's an Axios-like error structure (adjust based on your actual error structure)
        if (typeof error === 'object' && error !== null && 'response' in error) {
          const responseError = error as { response?: { data?: { message?: string } } }
          message = responseError.response?.data?.message || message
        }
        ElMessage.error(`更新失败: ${message}`)
      } finally {
        editLoading.value = false
      }
    }
  })
}

const submitReview = async (approved: boolean) => {
  if (!selectedCinema.value?.id) return

  // --- 开始校验管理员关联 (仅在批准时) ---
  if (approved) {
    if (!reviewForm.adminUserId) {
      ElMessage.warning('批准影院时必须选择一个关联的管理员。')
      return
    }
    const selectedUserId = Number(reviewForm.adminUserId)
    const isAlreadyAssigned = cinemas.value.some(
      (cinema) =>
        cinema.id !== selectedCinema.value!.id && // 理论上新申请的影院ID不在列表，但加上更保险
        cinema.adminUserId === selectedUserId,
    )
    if (isAlreadyAssigned) {
      ElMessage.error('你选择的用户已关联其它影院')
      return // 阻止提交
    }
  }
  // --- 结束校验管理员关联 ---

  reviewLoading.value = true
  try {
    const payload: CinemaReviewRequest = {
      approved: approved,
      // 如果批准，传递userId；如果拒绝，不传或传undefined（根据API调整，这里是拒绝时传undefined）
      adminUserId: approved ? Number(reviewForm.adminUserId) : undefined,
    }
    await reviewCinema(selectedCinema.value.id, payload)
    ElMessage.success(`影院已${approved ? '批准' : '拒绝'}`)
    reviewDialogVisible.value = false
    fetchCinemas() // Refresh list
  } catch (error: unknown) {
    console.error('审核影院失败:', error)
    let message = '未知错误'
    if (error instanceof Error) {
      message = error.message
    }
    if (typeof error === 'object' && error !== null && 'response' in error) {
      const responseError = error as { response?: { data?: { message?: string } } }
      message = responseError.response?.data?.message || message
    }
    ElMessage.error(`审核失败: ${message}`)
  } finally {
    reviewLoading.value = false
  }
}

const submitStatusChange = async () => {
  if (!statusFormRef.value || !selectedCinema.value?.id || !statusForm.status) return
  await statusFormRef.value.validate(async (valid) => {
    if (valid) {
      statusLoading.value = true
      try {
        const payload: CinemaStatusUpdateRequest = {
          status: statusForm.status as CinemaStatusUpdateRequest['status'], // Cast as it's validated
        }
        await updateCinemaStatus(selectedCinema.value!.id, payload)
        ElMessage.success('影院状态更新成功')
        statusDialogVisible.value = false
        fetchCinemas() // Refresh list
      } catch (error: unknown) {
        console.error('更新影院状态失败:', error)
        let message = '未知错误'
        if (error instanceof Error) {
          message = error.message
        }
        if (typeof error === 'object' && error !== null && 'response' in error) {
          const responseError = error as { response?: { data?: { message?: string } } }
          message = responseError.response?.data?.message || message
        }
        ElMessage.error(`更新状态失败: ${message}`)
      } finally {
        statusLoading.value = false
      }
    }
  })
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
    case 'DISABLED':
      return 'info'
    default:
      return 'info'
  }
}

const getStatusText = (status: string | undefined): string => {
  const option = cinemaStatusOptions.find((o) => o.value === status)
  return option ? option.label : status || '未知'
}

// 计算状态变更对话框中可用的选项
const availableStatusOptions = computed(() => {
  if (!selectedCinema.value) return []

  const currentStatus = selectedCinema.value.status

  // 基于当前状态过滤允许的目标状态
  switch (currentStatus) {
    case 'APPROVED':
      // 已批准的只能变更为禁用
      return cinemaStatusOptions.filter((o) => o.value === 'DISABLED')
    case 'DISABLED':
      // 已禁用的只能变更为批准
      return cinemaStatusOptions.filter((o) => o.value === 'APPROVED')
    case 'REJECTED':
      // 已拒绝的不能通过此对话框变更
      return []
    case 'PENDING_APPROVAL':
      // 待审核的不应使用此对话框，应使用审核流程
      return []
    default:
      // 默认不显示任何选项
      return []
  }
})

// --- Validation Rules ---
const editFormRules = reactive<FormRules>({
  name: [{ required: true, message: '请输入影院名称', trigger: 'blur' }],
  address: [{ required: true, message: '请输入影院地址', trigger: 'blur' }],
  // Add other rules as needed (e.g., phone format)
})

const reviewFormRules = reactive<FormRules>({
  // adminUserId is validated conditionally in the submit function
})

const statusFormRules = reactive<FormRules>({
  status: [{ required: true, message: '请选择新的状态', trigger: 'change' }],
})

// --- Lifecycle Hooks ---
onMounted(() => {
  fetchCinemas()
  // Optionally pre-fetch admin users if needed frequently
  // fetchAdminUsers();
})
</script>

<style scoped>
.cinema-management-view {
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
