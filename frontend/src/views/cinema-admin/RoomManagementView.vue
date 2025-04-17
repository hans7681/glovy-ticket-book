<template>
  <div class="room-management-view">
    <div class="page-header">
      <h2>影厅管理</h2>
      <el-button type="primary" @click="openAddDialog">
        <el-icon><Plus /></el-icon> 添加影厅
      </el-button>
    </div>

    <el-card class="table-card" shadow="never">
      <div class="table-header">
        <div class="table-title">影厅列表</div>
      </div>
      <el-table :data="rooms" v-loading="loading" border stripe class="room-table">
        <el-table-column prop="id" label="ID" width="80" align="center"></el-table-column>
        <el-table-column prop="name" label="影厅名称" min-width="120"></el-table-column>
        <el-table-column prop="rows" label="行数" width="100" align="center"></el-table-column>
        <el-table-column prop="cols" label="列数" width="100" align="center"></el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" align="center">
          <template #default="{ row }">
            {{ formatDateTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="updateTime" label="更新时间" width="180" align="center">
          <template #default="{ row }">
            {{ formatDateTime(row.updateTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right" align="center">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="openEditDialog(row)">
              <el-icon><Edit /></el-icon> 编辑
            </el-button>
            <el-button type="danger" size="small" @click="handleDeleteRoom(row.id)">
              <el-icon><Delete /></el-icon> 删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-alert
      v-if="error"
      title="加载或操作影厅失败"
      type="error"
      :description="error"
      show-icon
      closable
      @close="error = null"
      class="alert-message"
    ></el-alert>

    <!-- 添加/编辑 对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEditing ? '编辑影厅' : '添加影厅'"
      width="500px"
      @close="resetForm"
      center
    >
      <el-form :model="currentRoom" ref="roomFormRef" label-width="80px" :rules="roomRules">
        <el-form-item label="名称" prop="name">
          <el-input v-model="currentRoom.name" placeholder="请输入影厅名称"></el-input>
        </el-form-item>
        <el-form-item label="行数" prop="rows">
          <el-input-number
            v-model="currentRoom.rows"
            :min="1"
            :max="50"
            placeholder="请输入行数"
          ></el-input-number>
        </el-form-item>
        <el-form-item label="列数" prop="cols">
          <el-input-number
            v-model="currentRoom.cols"
            :min="1"
            :max="50"
            placeholder="请输入列数"
          ></el-input-number>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSubmitRoom" :loading="saving">{{
            isEditing ? '保存' : '添加'
          }}</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import { listMyRooms, addRoom, updateRoom, deleteRoom } from '@/services/cinemaAdminApi'
import type { Room } from '@/types/room'
import {
  ElMessage,
  ElMessageBox,
  ElTable,
  ElTableColumn,
  ElButton,
  ElDialog,
  ElForm,
  ElFormItem,
  ElInput,
  ElInputNumber,
  ElAlert,
} from 'element-plus'
import type { FormRules } from 'element-plus'
import { Plus, Edit, Delete } from '@element-plus/icons-vue'

type RoomFormInstance = InstanceType<typeof ElForm>

const rooms = ref<Room[]>([])
const loading = ref(true)
const saving = ref(false)
const error = ref<string | null>(null)
const dialogVisible = ref(false)
const isEditing = ref(false)
const currentRoom = ref<Partial<Room>>({}) // Use Partial for the form model
const roomFormRef = ref<RoomFormInstance>()

const roomRules = reactive<FormRules>({
  name: [{ required: true, message: '请输入影厅名称', trigger: 'blur' }],
  rows: [
    { required: true, message: '请输入行数', trigger: 'blur' },
    { type: 'number', min: 1, message: '行数必须大于0', trigger: 'blur' },
  ],
  cols: [
    { required: true, message: '请输入列数', trigger: 'blur' },
    { type: 'number', min: 1, message: '列数必须大于0', trigger: 'blur' },
  ],
})

const loadRooms = async () => {
  loading.value = true
  error.value = null
  try {
    rooms.value = await listMyRooms()
  } catch (err: unknown) {
    console.error('Failed to load rooms:', err)
    const apiError = err as { response?: { data?: { message?: string } }; message?: string }
    error.value =
      apiError.response?.data?.message || apiError.message || '无法加载影厅列表，请稍后再试。'
    ElMessage.error(error.value)
  } finally {
    loading.value = false
  }
}

const resetForm = () => {
  currentRoom.value = {}
  isEditing.value = false
  roomFormRef.value?.resetFields()
}

const openAddDialog = () => {
  resetForm()
  dialogVisible.value = true
}

const openEditDialog = (room: Room) => {
  // Make a copy for editing
  currentRoom.value = { id: room.id, name: room.name, rows: room.rows, cols: room.cols }
  isEditing.value = true
  dialogVisible.value = true
}

const handleSubmitRoom = async () => {
  if (!roomFormRef.value) return

  try {
    await roomFormRef.value.validate()
  } catch {
    ElMessage.error('请检查表单输入项！')
    return
  }

  saving.value = true
  error.value = null
  try {
    if (isEditing.value && currentRoom.value.id) {
      // Prepare update payload (only name, rows, cols are needed based on API type)
      const updatePayload: Pick<Room, 'name' | 'rows' | 'cols'> = {
        name: currentRoom.value.name || '',
        rows: currentRoom.value.rows || 1,
        cols: currentRoom.value.cols || 1,
      }
      await updateRoom(currentRoom.value.id, updatePayload)
      ElMessage.success('影厅更新成功！')
    } else {
      // Prepare add payload (exclude id, cinemaId, times etc.)
      const addPayload: Omit<
        Room,
        'id' | 'cinemaId' | 'createTime' | 'updateTime' | 'seatTemplate'
      > = {
        name: currentRoom.value.name || '',
        rows: currentRoom.value.rows || 1,
        cols: currentRoom.value.cols || 1,
      }
      await addRoom(addPayload)
      ElMessage.success('影厅添加成功！')
    }
    dialogVisible.value = false
    await loadRooms() // Refresh the list
  } catch (err: unknown) {
    console.error('Failed to save room:', err)
    const apiError = err as { response?: { data?: { message?: string } }; message?: string }
    error.value = apiError.response?.data?.message || apiError.message || '操作失败，请稍后再试。'
    ElMessage.error(error.value)
  } finally {
    saving.value = false
  }
}

const handleDeleteRoom = async (roomId: number) => {
  try {
    await ElMessageBox.confirm('确定要删除这个影厅吗？此操作不可恢复。', '警告', {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'warning',
    })
    // User confirmed
    loading.value = true // Show loading state during deletion
    error.value = null
    await deleteRoom(roomId)
    ElMessage.success('影厅删除成功！')
    await loadRooms() // Refresh list
  } catch (err: unknown) {
    // Handle cancellation or API error
    if (err === 'cancel') {
      ElMessage.info('已取消删除')
      return
    }
    console.error('Failed to delete room:', err)
    const apiError = err as { response?: { data?: { message?: string } }; message?: string }
    error.value = apiError.response?.data?.message || apiError.message || '删除失败，请稍后再试。'
    ElMessage.error(error.value)
  } finally {
    loading.value = false // Hide loading state
  }
}

const formatDateTime = (isoString: string | undefined | null): string => {
  if (!isoString) {
    return 'N/A'
  }
  try {
    const date = new Date(isoString)
    // Simple formatting: YYYY-MM-DD HH:mm:ss
    const year = date.getFullYear()
    const month = (date.getMonth() + 1).toString().padStart(2, '0')
    const day = date.getDate().toString().padStart(2, '0')
    const hours = date.getHours().toString().padStart(2, '0')
    const minutes = date.getMinutes().toString().padStart(2, '0')
    const seconds = date.getSeconds().toString().padStart(2, '0')
    return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`
  } catch (e) {
    console.error('Error formatting date:', isoString, e)
    return 'Invalid Date'
  }
}

onMounted(() => {
  loadRooms()
})
</script>

<style scoped>
.room-management-view {
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

.room-table {
  width: 100%;
}

.alert-message {
  margin-top: 20px;
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
