<template>
  <div class="my-cinema-view" v-loading="loading">
    <div class="page-header">
      <h2>我的影院信息</h2>
      <el-button type="primary" @click="toggleEdit" v-if="!isEditing && cinemaInfo">
        <el-icon><EditPen /></el-icon> 编辑
      </el-button>
    </div>

    <el-card class="content-card" shadow="never" v-if="!isEditing && cinemaInfo">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="影院名称">{{ cinemaInfo.name }}</el-descriptions-item>
        <el-descriptions-item label="影院地址">{{ cinemaInfo.address }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ cinemaInfo.phone }}</el-descriptions-item>
        <el-descriptions-item label="影院介绍">
          <div v-html="cinemaInfo.description?.replace(/\n/g, '<br>')"></div>
        </el-descriptions-item>
        <el-descriptions-item label="影院Logo">
          <el-image
            style="width: 100px; height: 100px"
            :src="cinemaInfo.logo || defaultLogo"
            :preview-src-list="[cinemaInfo.logo || defaultLogo]"
            fit="contain"
            lazy
          >
            <template #error>
              <div class="image-slot">
                <el-icon><Picture /></el-icon>
              </div>
            </template>
          </el-image>
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getStatusTagType(cinemaInfo.status)">{{
            getStatusText(cinemaInfo.status)
          }}</el-tag>
        </el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card class="content-card" shadow="never" v-if="isEditing && cinemaInfoCopy">
      <div class="card-header">
        <div class="card-title">编辑影院信息</div>
      </div>
      <el-form
        :model="cinemaInfoCopy"
        ref="cinemaFormRef"
        label-width="100px"
        @submit.prevent="saveCinemaInfo"
        class="edit-form"
      >
        <el-form-item
          label="影院名称"
          prop="name"
          :rules="[{ required: true, message: '请输入影院名称', trigger: 'blur' }]"
        >
          <el-input v-model="cinemaInfoCopy.name"></el-input>
        </el-form-item>
        <el-form-item
          label="影院地址"
          prop="address"
          :rules="[{ required: true, message: '请输入影院地址', trigger: 'blur' }]"
        >
          <el-input v-model="cinemaInfoCopy.address"></el-input>
        </el-form-item>
        <el-form-item label="联系电话" prop="phone">
          <el-input v-model="cinemaInfoCopy.phone"></el-input>
        </el-form-item>
        <el-form-item label="影院介绍" prop="description">
          <el-input type="textarea" :rows="4" v-model="cinemaInfoCopy.description"></el-input>
        </el-form-item>
        <el-form-item label="Logo URL" prop="logo">
          <el-input v-model="cinemaInfoCopy.logo"></el-input>
          <el-image
            v-if="cinemaInfoCopy.logo"
            style="width: 100px; height: 100px; margin-top: 10px"
            :src="cinemaInfoCopy.logo"
            fit="contain"
          />
          <div v-else class="image-placeholder">输入URL后显示预览</div>
          <el-text type="info" size="small" style="margin-top: 5px">
            (未来将支持直接上传图片文件)
          </el-text>
        </el-form-item>

        <el-form-item class="form-buttons">
          <el-button type="primary" @click="saveCinemaInfo" :loading="saving">保存</el-button>
          <el-button @click="cancelEdit">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-alert
      v-if="error"
      title="加载影院信息失败"
      type="error"
      :description="error"
      show-icon
      closable
      @close="error = null"
      class="alert-message"
    ></el-alert>

    <el-alert
      v-if="!loading && !cinemaInfo && !error"
      type="info"
      show-icon
      :closable="false"
      class="alert-message"
    >
      您尚未关联或申请影院。
    </el-alert>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getMyCinema, updateMyCinema } from '@/services/cinemaAdminApi'
import type { Cinema } from '@/types/cinema'
import {
  ElMessage,
  ElForm,
  // ElCard, // Removed ElCard import
  ElButton,
  ElDescriptions,
  ElDescriptionsItem,
  ElFormItem,
  ElInput,
  ElTag,
  ElAlert,
  ElImage,
  ElIcon,
  ElText,
  // No TagType import needed
} from 'element-plus'
import { EditPen, Picture } from '@element-plus/icons-vue'
// import defaultLogo from '@/assets/images/default-cinema-logo.png' // Ensure you have a default logo
import defaultLogo from '@/assets/logo.svg'

// Define a more specific error type structure if possible, based on AxiosError or fetch response
interface ApiError {
  response?: {
    status?: number
    data?: {
      message?: string
    }
  }
  message?: string
}

type FormInstance = InstanceType<typeof ElForm>

const cinemaInfo = ref<Cinema | null>(null)
const cinemaInfoCopy = ref<Partial<Cinema> | null>(null) // Use Partial for edit copy
const loading = ref(true)
const isEditing = ref(false)
const saving = ref(false)
const error = ref<string | null>(null)
const cinemaFormRef = ref<FormInstance>()

const loadCinemaInfo = async () => {
  loading.value = true
  error.value = null
  try {
    cinemaInfo.value = await getMyCinema()
  } catch (err: unknown) {
    console.error('Failed to load cinema info:', err)
    const apiError = err as ApiError // Cast to the defined interface
    let message = '无法加载影院信息，请稍后再试。'

    // Check if it's a 404 specifically for 'not found'
    if (apiError.response?.status === 404) {
      message = '您尚未关联或申请影院。'
      error.value = null // Treat 404 not as a blocking error, but info
      cinemaInfo.value = null // Ensure cinemaInfo is null
    } else {
      // Use the message from the API response if available
      message = apiError.response?.data?.message || apiError.message || message
      error.value = message
      ElMessage.error(error.value)
    }
  } finally {
    loading.value = false
  }
}

const toggleEdit = () => {
  if (cinemaInfo.value) {
    // Create a deep copy for editing to avoid modifying the original object directly
    // Using Partial<Cinema> for cinemaInfoCopy makes this safer
    cinemaInfoCopy.value = JSON.parse(JSON.stringify(cinemaInfo.value))
    isEditing.value = true
  } else {
    ElMessage.warning('无法进入编辑模式，影院信息尚未加载。')
  }
}

const cancelEdit = () => {
  isEditing.value = false
  cinemaInfoCopy.value = null // Clear the copy
  cinemaFormRef.value?.resetFields() // Reset form validation
}

const saveCinemaInfo = async () => {
  if (!cinemaFormRef.value || !cinemaInfoCopy.value) return

  // Ensure the original ID exists before attempting to save
  const originalId = cinemaInfo.value?.id
  if (typeof originalId !== 'number') {
    ElMessage.error('无法获取影院ID，无法保存。')
    saving.value = false
    return
  }

  try {
    await cinemaFormRef.value.validate()
  } catch {
    ElMessage.error('请检查表单输入项！')
    return // Stop if validation fails
  }

  saving.value = true
  error.value = null
  try {
    // Prepare payload: Use original ID and edited data
    // Ensure payload matches the expected type for the API function (might need refinement)
    const payload: Cinema = {
      ...(cinemaInfoCopy.value as Cinema), // Cast might be needed if API expects full Cinema
      id: originalId, // Use the validated original ID
    }

    // Remove fields that shouldn't be sent or are managed by backend
    // Adjust based on your actual API requirements for the update endpoint
    delete payload.createTime
    delete payload.updateTime
    delete payload.adminUserId
    // delete payload.status; // Status might be updated via a different endpoint or flow

    const updatedCinema = await updateMyCinema(payload) // Pass the correct ID
    cinemaInfo.value = updatedCinema // Update the display data
    isEditing.value = false // Exit edit mode
    cinemaInfoCopy.value = null // Clear the copy
    ElMessage.success('影院信息更新成功！')
  } catch (err: unknown) {
    console.error('Failed to save cinema info:', err)
    const apiError = err as ApiError // Cast to the defined interface
    let message = '保存失败，请稍后再试。'
    message = apiError.response?.data?.message || apiError.message || message
    error.value = message
    ElMessage.error(error.value)
  } finally {
    saving.value = false
  }
}

const getStatusText = (status: Cinema['status'] | undefined | null) => {
  switch (status) {
    case 'PENDING_APPROVAL':
      return '待审核'
    case 'APPROVED':
      return '已批准'
    case 'REJECTED':
      return '已拒绝'
    case 'DISABLED':
      return '已禁用'
    default:
      return '未知'
  }
}

// Helper function to determine tag type based on status
const getStatusTagType = (
  status: Cinema['status'] | undefined | null,
): 'success' | 'warning' | 'danger' | 'info' => {
  switch (status) {
    case 'APPROVED':
      return 'success'
    case 'PENDING_APPROVAL':
      return 'warning'
    case 'REJECTED':
      return 'danger'
    case 'DISABLED':
      return 'info'
    default:
      return 'info'
  }
}

onMounted(() => {
  loadCinemaInfo()
})
</script>

<style scoped>
.my-cinema-view {
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

.content-card {
  margin-bottom: 20px;
  overflow: visible;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.card-title {
  font-size: 16px;
  font-weight: bold;
  color: #303133;
}

.edit-form {
  max-width: 800px;
}

.form-buttons {
  margin-top: 20px;
}

.alert-message {
  margin-top: 20px;
}

.image-placeholder {
  width: 100px;
  height: 100px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
  border: 1px dashed #dcdfe6;
  margin-top: 10px;
  color: #909399;
  font-size: 12px;
  text-align: center;
}

:deep(.el-descriptions__label) {
  width: 120px;
  background-color: #f5f7fa;
}

:deep(.el-card__body) {
  padding: 15px;
  overflow: visible !important;
}
</style>
