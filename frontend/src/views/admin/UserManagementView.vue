<template>
  <div class="user-management">
    <div class="page-header">
      <h2>用户管理</h2>
    </div>

    <!-- 搜索/筛选区域 -->
    <el-card class="filter-card" shadow="never">
      <el-form :inline="true" :model="searchParams">
        <el-form-item label="用户名">
          <el-input
            v-model="searchParams.username"
            placeholder="输入用户名搜索"
            clearable
            @clear="handleSearch"
            style="width: 220px"
          ></el-input>
        </el-form-item>
        <el-form-item label="角色">
          <el-select
            v-model="searchParams.role"
            placeholder="选择角色"
            clearable
            @clear="handleSearch"
            style="width: 180px"
          >
            <el-option label="普通用户" value="USER"></el-option>
            <el-option label="影院管理员" value="CINEMA_ADMIN"></el-option>
            <el-option label="系统管理员" value="SYSTEM_ADMIN"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon> 查询
          </el-button>
          <el-button @click="resetSearch">
            <el-icon><Refresh /></el-icon> 重置
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 表格区域 -->
    <el-card class="table-card" shadow="never">
      <div class="table-header">
        <div class="table-title">用户列表</div>
      </div>

      <el-table :data="users" v-loading="loading" style="width: 100%" stripe border size="default">
        <el-table-column prop="id" label="ID" width="80" align="center"></el-table-column>
        <el-table-column prop="username" label="用户名" min-width="120"></el-table-column>
        <el-table-column prop="nickname" label="昵称" min-width="120"></el-table-column>
        <el-table-column prop="phone" label="手机号" min-width="120"></el-table-column>
        <el-table-column prop="email" label="邮箱" min-width="180"></el-table-column>
        <el-table-column prop="role" label="角色" width="120" align="center">
          <template #default="scope">
            <el-tag :type="getRoleTagType(scope.row.role)">{{ formatRole(scope.row.role) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" min-width="160" align="center">
          <template #default="scope">
            {{ formatDateTime(scope.row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right" align="center">
          <template #default="scope">
            <el-button size="small" type="primary" @click="handleEdit(scope.row)">
              <el-icon><Edit /></el-icon> 编辑
            </el-button>
            <el-button size="small" type="danger" @click="handleDelete(scope.row)">
              <el-icon><Delete /></el-icon> 删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页区域 -->
      <el-pagination
        v-if="pagination.total > 0"
        background
        layout="total, sizes, prev, pager, next, jumper"
        :total="pagination.total"
        :page-sizes="[10, 20, 50, 100]"
        :page-size="pagination.size"
        :current-page="pagination.current"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
        class="pagination-container"
      >
      </el-pagination>
    </el-card>

    <!-- 编辑对话框 -->
    <el-dialog v-model="editDialogVisible" title="编辑用户" width="500px" center>
      <el-form :model="currentUser" ref="editFormRef" label-width="80px" label-position="right">
        <el-form-item label="用户名">
          <el-input :value="currentUser.username" disabled style="width: 100%"></el-input>
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="currentUser.nickname" style="width: 100%"></el-input>
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="currentUser.phone" style="width: 100%"></el-input>
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="currentUser.email" style="width: 100%"></el-input>
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="currentUser.role" placeholder="选择角色" style="width: 100%">
            <el-option label="普通用户" value="USER"></el-option>
            <el-option label="影院管理员" value="CINEMA_ADMIN"></el-option>
            <el-option label="系统管理员" value="SYSTEM_ADMIN"></el-option>
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="editDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSave">保存</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import {
  ElTable,
  ElTableColumn,
  ElPagination,
  ElCard,
  ElForm,
  ElFormItem,
  ElInput,
  ElSelect,
  ElOption,
  ElButton,
  ElDialog,
  ElTag,
  ElIcon,
  ElMessage,
  ElMessageBox,
} from 'element-plus'
import { Search, Edit, Delete, Refresh } from '@element-plus/icons-vue' // Import icons
import { getUsers, updateUser, deleteUser } from '@/services/adminApi' // Import admin API functions
import type { User } from '@/types/user' // Assuming User type path
import type { FormInstance } from 'element-plus'
import { format, parseISO } from 'date-fns' // For date formatting

// Reactive states
const users = ref<User[]>([])
const loading = ref(false)
const pagination = reactive({
  current: 1,
  size: 10,
  total: 0,
})
const searchParams = reactive({
  username: '',
  role: '' as 'USER' | 'CINEMA_ADMIN' | 'SYSTEM_ADMIN' | '',
})

// Edit Dialog states
const editDialogVisible = ref(false)
const currentUser = ref<Partial<User>>({}) // Use Partial<User> for the form model
const editFormRef = ref<FormInstance | null>(null)

// --- Helper Functions ---
const formatRole = (role: string | null | undefined): string => {
  switch (role) {
    case 'USER':
      return '普通用户'
    case 'CINEMA_ADMIN':
      return '影院管理员'
    case 'SYSTEM_ADMIN':
      return '系统管理员'
    default:
      return '未知'
  }
}

const getRoleTagType = (
  role: string | null | undefined,
): 'success' | 'info' | 'warning' | 'danger' => {
  switch (role) {
    case 'USER':
      return 'info'
    case 'CINEMA_ADMIN':
      return 'success'
    case 'SYSTEM_ADMIN':
      return 'danger'
    default:
      return 'info' // Return 'info' as default instead of ''
  }
}

const formatDateTime = (dateTimeString: string | null | undefined): string => {
  if (!dateTimeString) return '-'
  try {
    return format(parseISO(dateTimeString), 'yyyy-MM-dd HH:mm:ss')
  } catch (e) {
    console.error('Error formatting date:', e)
    return dateTimeString // Fallback to original string if formatting fails
  }
}

// --- Core Functions ---
const fetchData = async () => {
  loading.value = true
  try {
    const params = {
      current: pagination.current,
      size: pagination.size,
      username: searchParams.username || undefined, // Send undefined if empty
      role: searchParams.role || undefined,
    }
    const data = await getUsers(params)
    users.value = data.records || []
    pagination.total = data.total || 0
  } catch (error) {
    console.error('Error fetching users:', error)
    ElMessage.error('获取用户列表失败')
    users.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.current = 1 // Reset to first page on search
  fetchData()
}

const handleSizeChange = (newSize: number) => {
  pagination.size = newSize
  fetchData()
}

const handleCurrentChange = (newPage: number) => {
  pagination.current = newPage
  fetchData()
}

const handleEdit = (user: User) => {
  // Create a deep copy to avoid modifying the original table data directly
  currentUser.value = JSON.parse(JSON.stringify(user))
  editDialogVisible.value = true
}

const handleSave = async () => {
  if (!editFormRef.value) return

  try {
    const valid = await editFormRef.value.validate()
    if (valid && currentUser.value.id) {
      loading.value = true // Optional: show loading state during save
      await updateUser(currentUser.value.id, currentUser.value)
      ElMessage.success('用户信息更新成功')
      editDialogVisible.value = false
      fetchData() // Refresh the list
    } else {
      console.log('Form validation failed or user ID missing')
    }
  } catch (error) {
    ElMessage.error('更新用户信息失败')
    console.error('Error saving user:', error)
  } finally {
    loading.value = false
  }
}

const handleDelete = async (user: User) => {
  if (!user.id) return

  try {
    await ElMessageBox.confirm(
      `确定要删除用户 "${user.username}" 吗？此操作通常是逻辑删除，不会物理移除数据。`,
      '确认删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      },
    )

    // User confirmed
    loading.value = true
    await deleteUser(user.id)
    ElMessage.success('用户删除成功')
    fetchData() // Refresh the list
  } catch {
    // If error is 'cancel', ElMessageBox.confirm throws 'cancel', which we don't treat as an error here.
    // We only show the ElMessage error for other exceptions.
    // The previous check 'if (error !== 'cancel')' is implicitly handled now,
    // because only actual errors will proceed to this catch block.
    ElMessage.error('删除用户失败')
    // console.error can be removed if detailed logging isn't needed, or kept without the 'error' var
    console.error(`Failed to delete user ${user.username} (ID: ${user.id})`)
  } finally {
    loading.value = false
  }
}

// 添加重置搜索功能
const resetSearch = () => {
  searchParams.username = ''
  searchParams.role = ''
  handleSearch()
}

// Lifecycle hook
onMounted(() => {
  fetchData() // Load initial data when component mounts
})
</script>

<style scoped>
.user-management {
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
