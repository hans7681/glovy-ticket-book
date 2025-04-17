<template>
  <div class="movie-type-management">
    <el-card class="table-card" shadow="never">
      <div class="table-header">
        <div class="table-title">电影类型列表</div>
        <div class="actions">
          <el-button type="primary" @click="handleAddType">
            <el-icon><Plus /></el-icon> 新增类型
          </el-button>
        </div>
      </div>

      <el-table :data="types" v-loading="typeLoading" style="width: 100%" stripe border>
        <el-table-column prop="id" label="ID" width="80" align="center"></el-table-column>
        <el-table-column prop="name" label="类型名称" min-width="180"></el-table-column>
        <el-table-column
          prop="tmdbGenreId"
          label="TMDB ID"
          width="120"
          align="center"
        ></el-table-column>
        <el-table-column label="操作" width="180" fixed="right" align="center">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click="handleEditType(row)">
              <el-icon><Edit /></el-icon> 编辑
            </el-button>
            <el-button size="small" type="danger" @click="handleDeleteType(row.id)">
              <el-icon><Delete /></el-icon> 删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- Add/Edit Dialog -->
    <el-dialog
      v-model="typeDialogVisible"
      :title="currentType.id ? '编辑电影类型' : '新增电影类型'"
      width="500px"
      @close="resetTypeForm"
      center
    >
      <el-form
        ref="typeFormRef"
        :model="currentType"
        :rules="typeFormRules"
        label-width="100px"
        label-position="right"
      >
        <el-form-item label="类型名称" prop="name">
          <el-input
            v-model="currentType.name"
            placeholder="请输入类型名称"
            style="width: 100%"
          ></el-input>
        </el-form-item>
        <el-form-item label="TMDB ID" prop="tmdbGenreId">
          <el-input
            v-model.number="currentType.tmdbGenreId"
            type="number"
            placeholder="请输入关联的 TMDB Genre ID (可选)"
            style="width: 100%"
          ></el-input>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="typeDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitTypeForm">确定</el-button>
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
  ElButton,
  ElDialog,
  ElForm,
  ElFormItem,
  ElInput,
  ElMessageBox,
  ElMessage,
  ElCard,
  ElIcon,
  type FormInstance,
  type FormRules,
} from 'element-plus'
import { Edit, Delete, Plus } from '@element-plus/icons-vue'
import { getMovieTypes, addMovieType, updateMovieType, deleteMovieType } from '@/services/api' // Adjust path if needed
import type { MovieType } from '@/types/movieType' // Adjust path if needed
import axios from 'axios'

// State
const types = ref<MovieType[]>([])
const typeLoading = ref(true)
const typeDialogVisible = ref(false)
const typeFormRef = ref<FormInstance>()
const currentType = reactive<Partial<MovieType>>({
  // Use Partial for create/edit form
  name: '',
  tmdbGenreId: null,
})

// Validation Rules
const typeFormRules = reactive<FormRules>({
  name: [{ required: true, message: '请输入类型名称', trigger: 'blur' }],
})

// Fetch Data
const fetchTypes = async () => {
  typeLoading.value = true
  try {
    const response = await getMovieTypes()
    types.value = response.data
  } catch (error) {
    console.error('Failed to fetch movie types:', error)
    ElMessage.error('加载电影类型失败')
  } finally {
    typeLoading.value = false
  }
}

onMounted(() => {
  fetchTypes()
})

// Reset Form
const resetTypeForm = () => {
  Object.assign(currentType, { id: undefined, name: '', tmdbGenreId: null }) // Reset all fields
  typeFormRef.value?.clearValidate() // Clear validation errors
}

// Add Type
const handleAddType = () => {
  resetTypeForm()
  typeDialogVisible.value = true
}

// Edit Type
const handleEditType = (type: MovieType) => {
  // Create a copy to avoid modifying the original list item directly
  Object.assign(currentType, { ...type })
  typeDialogVisible.value = true
}

// Delete Type
const handleDeleteType = async (id: number) => {
  try {
    await ElMessageBox.confirm('确定要删除这个电影类型吗？此操作不可恢复。', '确认删除', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await deleteMovieType(id)
    ElMessage.success('电影类型删除成功')
    await fetchTypes() // 刷新列表
  } catch (error) {
    if (error === 'cancel') {
      ElMessage.info('已取消删除')
      return
    }
    console.error('Failed to delete movie type:', error)
    if (axios.isAxiosError(error) && error.response?.status === 400) {
      ElMessage.error(`删除失败: ${error.response.data || '该类型可能被电影引用，无法删除'}`)
    } else {
      ElMessage.error('删除电影类型失败')
    }
  }
}

// Submit Form (Add/Edit)
const submitTypeForm = async () => {
  if (!typeFormRef.value) return
  await typeFormRef.value.validate(async (valid) => {
    if (valid) {
      try {
        const dataToSubmit: Partial<Omit<MovieType, 'id'>> = {
          name: currentType.name,
          // Send null if empty or 0, adjust based on backend expectation
          tmdbGenreId: currentType.tmdbGenreId || null,
        }

        if (currentType.id) {
          // Update
          await updateMovieType(currentType.id, dataToSubmit)
          ElMessage.success('电影类型更新成功')
        } else {
          // Add
          await addMovieType(dataToSubmit as Omit<MovieType, 'id'>) // Assert type for add
          ElMessage.success('电影类型添加成功')
        }
        typeDialogVisible.value = false
        await fetchTypes() // Refresh list
        // TODO: Emit an event or use a store to notify MovieManagementTab to update its types list
      } catch (error) {
        console.error('Failed to save movie type:', error)
        if (axios.isAxiosError(error) && error.response?.status === 400) {
          ElMessage.error(`保存失败: ${error.response.data || '类型名称可能已存在'}`)
        } else {
          ElMessage.error('保存电影类型失败')
        }
      }
    }
  })
}
</script>

<style scoped>
.movie-type-management {
  height: auto;
  overflow: visible;
  width: 100%;
}

.table-card {
  margin-bottom: 20px;
  height: auto;
  overflow: visible;
  width: 100%;
}

/* 对Element Plus表格组件进行样式调整，防止出现不必要的滚动条 */
:deep(.el-table) {
  width: 100% !important;
  overflow: visible;
}

:deep(.el-table__body-wrapper) {
  overflow: visible;
}

:deep(.el-card__body) {
  overflow: visible;
  padding: 15px;
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

.actions {
  display: flex;
  justify-content: flex-end;
}

.dialog-footer {
  width: 100%;
  display: flex;
  justify-content: flex-end;
}
</style>
