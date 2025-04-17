<template>
  <div class="application-form-container">
    <el-form
      ref="applicationFormRef"
      :model="formData"
      :rules="formRules"
      label-width="120px"
      @submit.prevent="submitApplication"
    >
      <el-form-item label="影院名称" prop="name">
        <el-input v-model="formData.name" placeholder="请输入影院名称"></el-input>
      </el-form-item>
      <el-form-item label="影院地址" prop="address">
        <el-input v-model="formData.address" placeholder="请输入详细地址"></el-input>
      </el-form-item>
      <el-form-item label="联系电话" prop="phone">
        <el-input v-model="formData.phone" placeholder="请输入11位手机号码"></el-input>
      </el-form-item>
      <el-form-item label="简要说明" prop="description">
        <el-input
          v-model="formData.description"
          type="textarea"
          :rows="4"
          placeholder="可选，可简要说明影院情况或合作意向"
        ></el-input>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" native-type="submit" :loading="isSubmitting">
          提交申请
        </el-button>
        <el-button @click="resetForm">重置</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, defineEmits } from 'vue'
import { ElForm, ElFormItem, ElInput, ElButton, ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import {
  submitCinemaApplication,
  type CinemaPartnershipApplicationRequestDTO,
} from '@/services/api' // 导入类型和API函数
import axios from 'axios'

// 定义事件，用于通知父组件提交结果
const emit = defineEmits(['submit-success', 'submit-error'])

const applicationFormRef = ref<FormInstance>()
const isSubmitting = ref(false)

const formData = reactive({
  name: '',
  address: '',
  phone: '',
  description: '',
})

const formRules = reactive<FormRules>({
  name: [{ required: true, message: '请输入影院名称', trigger: 'blur' }],
  address: [{ required: true, message: '请输入影院地址', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入联系电话', trigger: 'blur' },
    { pattern: /^\d{11}$/, message: '请输入11位手机号码', trigger: 'blur' },
  ],
})

const submitApplication = async () => {
  if (!applicationFormRef.value) return

  await applicationFormRef.value.validate(async (valid) => {
    if (valid) {
      isSubmitting.value = true
      try {
        const applicationData: CinemaPartnershipApplicationRequestDTO = {
          name: formData.name,
          address: formData.address,
          phone: formData.phone || undefined,
          description: formData.description || undefined,
        }

        await submitCinemaApplication(applicationData)

        ElMessage.success('申请已提交，请等待管理员审核。')
        applicationFormRef.value?.resetFields()
        emit('submit-success') // 触发成功事件
      } catch (error: unknown) {
        console.error('Cinema application submission failed:', error)
        let errorMessage = '提交申请失败，请稍后重试。'
        if (axios.isAxiosError(error)) {
          if (error.response?.data?.message) {
            errorMessage = `提交失败：${error.response.data.message}`
          } else if (error.response?.status === 409) {
            errorMessage = '提交失败：您可能已提交过申请或已经是影院管理员。'
          }
        }
        ElMessage.error(errorMessage)
        emit('submit-error', errorMessage) // 触发失败事件
      } finally {
        isSubmitting.value = false
      }
    } else {
      ElMessage.error('请检查表单填写是否正确。')
    }
  })
}

const resetForm = () => {
  applicationFormRef.value?.resetFields()
}
</script>

<style scoped>
.application-form-container {
  padding: 20px;
}
</style>
