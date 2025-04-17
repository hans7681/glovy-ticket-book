<template>
  <div class="announcement-management-container">
    <h2 class="page-header">公告管理</h2>

    <!-- 筛选表单 -->
    <el-card class="filter-card" shadow="never">
      <el-form :model="searchForm" label-width="80px" inline>
        <el-form-item label="标题">
          <el-input
            v-model="searchForm.title"
            placeholder="请输入公告标题"
            clearable
            :style="{ width: '200px' }"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select
            v-model="searchForm.published"
            placeholder="请选择状态"
            clearable
            :style="{ width: '120px' }"
          >
            <el-option :value="true" label="已发布" />
            <el-option :value="false" label="未发布" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="searchAnnouncements" :icon="Search"> 查询 </el-button>
          <el-button @click="resetSearch" :icon="RefreshRight"> 重置 </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 公告列表 -->
    <el-card class="table-card" shadow="never">
      <div class="table-header">
        <span class="table-title">公告列表</span>
        <el-button type="primary" @click="showCreateDialog" :icon="Plus"> 新建公告 </el-button>
      </div>

      <el-table :data="announcements" v-loading="loading" stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" align="center" />
        <el-table-column prop="title" label="标题" min-width="180" />
        <el-table-column prop="content" label="内容" min-width="250" show-overflow-tooltip />
        <el-table-column prop="createTime" label="创建时间" width="180" align="center" />
        <el-table-column prop="isPublished" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.isPublished ? 'success' : 'info'">
              {{ row.isPublished ? '已发布' : '未发布' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="240" align="center">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button size="small" type="primary" @click="showEditDialog(row)" :icon="Edit">
                编辑
              </el-button>
              <el-button
                size="small"
                :type="row.isPublished ? 'warning' : 'success'"
                @click="togglePublishStatus(row)"
              >
                {{ row.isPublished ? '取消发布' : '发布' }}
              </el-button>
              <el-button size="small" type="danger" @click="handleDelete(row)" :icon="Delete">
                删除
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-container" v-if="total > 0">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 30, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
          background
        />
      </div>
    </el-card>

    <!-- 创建/编辑公告对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑公告' : '新建公告'"
      width="40%"
      destroy-on-close
      center
    >
      <el-form ref="formRef" :model="formData" :rules="rules" label-width="80px" status-icon>
        <el-form-item label="标题" prop="title">
          <el-input v-model="formData.title" placeholder="请输入公告标题" />
        </el-form-item>
        <el-form-item label="内容" prop="content">
          <el-input
            v-model="formData.content"
            type="textarea"
            :rows="6"
            placeholder="请输入公告内容"
          />
        </el-form-item>
        <el-form-item label="是否发布" prop="isPublished">
          <el-switch v-model="formData.isPublished" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取 消</el-button>
          <el-button type="primary" @click="submitForm" :loading="submitLoading"> 确 定 </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 删除确认对话框 -->
    <el-dialog v-model="deleteConfirmVisible" title="删除确认" width="30%" center>
      <div class="delete-confirm">
        <el-icon :size="24" color="#e6a23c" class="warning-icon"><warning-filled /></el-icon>
        <span class="delete-message">您确定要删除该公告吗？此操作不可逆。</span>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="cancelDelete">取 消</el-button>
          <el-button type="danger" @click="confirmDelete" :loading="deleteLoading">
            确定删除
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  ElCard,
  ElForm,
  ElFormItem,
  ElInput,
  ElButton,
  ElTable,
  ElTableColumn,
  ElPagination,
  ElDialog,
  ElTag,
  ElSwitch,
  ElSelect,
  ElOption,
  ElIcon,
} from 'element-plus'
import { Search, RefreshRight, Edit, Delete, Plus, WarningFilled } from '@element-plus/icons-vue'
import type { FormInstance } from 'element-plus'
import {
  listAdminAnnouncements,
  addAnnouncement,
  updateAnnouncement,
  deleteAnnouncement as deleteAnnouncementApi,
  publishAnnouncement,
} from '@/services/api'
import type { Announcement } from '@/types/announcement'

// 过滤表单
const searchForm = reactive({
  title: '',
  published: undefined as boolean | undefined,
})

// 重置过滤
const resetSearch = () => {
  searchForm.title = ''
  searchForm.published = undefined
  searchAnnouncements()
}

// 表单参数
const formRef = ref<FormInstance>()
const formData = reactive({
  id: null as number | null,
  title: '',
  content: '',
  isPublished: false,
})

// 表单验证规则
const rules = {
  title: [
    { required: true, message: '请输入公告标题', trigger: 'blur' },
    { min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur' },
  ],
  content: [
    { required: true, message: '请输入公告内容', trigger: 'blur' },
    { min: 5, max: 1000, message: '长度在 5 到 1000 个字符', trigger: 'blur' },
  ],
}

// 分页参数
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 数据列表
const announcements = ref<Announcement[]>([])
const loading = ref(false)

// 对话框控制
const dialogVisible = ref(false)
const deleteConfirmVisible = ref(false)
const isEdit = ref(false)
const currentAnnouncement = ref<Announcement | null>(null)

// 加载状态
const submitLoading = ref(false)
const deleteLoading = ref(false)

// 删除确认
const announcementToDelete = ref<number | null>(null)

// 获取数据
const searchAnnouncements = async () => {
  loading.value = true
  try {
    const params = {
      current: page.value,
      size: pageSize.value,
      title: searchForm.title || undefined,
      isPublished: searchForm.published,
    }

    const res = await listAdminAnnouncements(params)
    announcements.value = res.data.records || []
    total.value = res.data.total || 0
  } catch (error) {
    console.error('获取公告列表失败', error)
    ElMessage.error('获取公告列表失败')
  } finally {
    loading.value = false
  }
}

// 分页处理
const handleSizeChange = (val: number) => {
  pageSize.value = val
  searchAnnouncements()
}

const handleCurrentChange = (val: number) => {
  page.value = val
  searchAnnouncements()
}

// 显示创建对话框
const showCreateDialog = () => {
  isEdit.value = false
  formData.id = null
  formData.title = ''
  formData.content = ''
  formData.isPublished = false
  dialogVisible.value = true
  // 在下一个事件循环中重置表单的验证状态
  setTimeout(() => {
    formRef.value?.resetFields()
  }, 0)
}

// 显示编辑对话框
const showEditDialog = (row: Announcement) => {
  isEdit.value = true
  formData.id = row.id
  formData.title = row.title
  formData.content = row.content
  formData.isPublished = row.isPublished
  dialogVisible.value = true
  // 在下一个事件循环中重置表单的验证状态
  setTimeout(() => {
    formRef.value?.resetFields()
  }, 0)
}

// 表单验证
const validateForm = async (formEl: FormInstance | undefined) => {
  if (!formEl) return false
  try {
    await formEl.validate()
    return true
  } catch {
    return false
  }
}

// 提交表单
const submitForm = async () => {
  const valid = await validateForm(formRef.value)
  if (!valid) return

  submitLoading.value = true
  try {
    if (isEdit.value && formData.id) {
      await updateAnnouncement(formData.id, {
        title: formData.title,
        content: formData.content,
      })
      if (formData.isPublished !== currentAnnouncement.value?.isPublished) {
        await publishAnnouncement(formData.id, { publish: formData.isPublished })
      }
      ElMessage.success('更新公告成功')
    } else {
      await addAnnouncement({
        title: formData.title,
        content: formData.content,
      })
      ElMessage.success('创建公告成功')
    }
    dialogVisible.value = false
    searchAnnouncements()
  } catch (error) {
    console.error('保存公告失败', error)
    ElMessage.error('保存公告失败')
  } finally {
    submitLoading.value = false
  }
}

// 删除公告
const handleDelete = (row: Announcement) => {
  deleteConfirmVisible.value = true
  announcementToDelete.value = row.id
}

// 确认删除
const confirmDelete = async () => {
  if (!announcementToDelete.value) return

  deleteLoading.value = true
  try {
    await deleteAnnouncementApi(announcementToDelete.value)
    ElMessage.success('删除公告成功')
    deleteConfirmVisible.value = false
    announcementToDelete.value = null
    searchAnnouncements()
  } catch (error) {
    console.error('删除公告失败', error)
    ElMessage.error('删除公告失败')
  } finally {
    deleteLoading.value = false
  }
}

// 取消删除
const cancelDelete = () => {
  deleteConfirmVisible.value = false
  announcementToDelete.value = null
}

// 切换发布状态
const togglePublishStatus = async (row: Announcement) => {
  const { id, isPublished } = row
  try {
    await publishAnnouncement(id, { publish: !isPublished })
    row.isPublished = !isPublished
    ElMessage.success(`${isPublished ? '取消发布' : '发布'}成功`)
    searchAnnouncements()
  } catch (error) {
    console.error(`${isPublished ? '取消发布' : '发布'}失败`, error)
    ElMessage.error(`${isPublished ? '取消发布' : '发布'}失败`)
  }
}

onMounted(() => {
  searchAnnouncements()
})
</script>

<style scoped>
.announcement-management-container {
  padding: 20px;
}

.page-header {
  margin-bottom: 20px;
  font-size: 22px;
  font-weight: 600;
  color: #303133;
}

.filter-card {
  margin-bottom: 20px;
}

.filter-card :deep(.el-card__body) {
  padding: 15px 20px;
}

.table-card {
  margin-bottom: 20px;
}

.table-card :deep(.el-card__body) {
  padding: 0 0 15px 0;
}

.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px 20px;
  border-bottom: 1px solid #ebeef5;
}

.table-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.pagination-container {
  display: flex;
  justify-content: flex-end;
  padding: 15px 20px 0;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
}

.delete-confirm {
  display: flex;
  align-items: center;
  padding: 10px 0;
}

.warning-icon {
  margin-right: 10px;
}

.delete-message {
  font-size: 16px;
}

.action-buttons {
  display: flex;
  justify-content: space-between;
  gap: 5px;
}
</style>
