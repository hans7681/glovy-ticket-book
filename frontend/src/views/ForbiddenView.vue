<template>
  <div class="forbidden-container">
    <el-result icon="error" title="403 - 访问被禁止" sub-title="抱歉，您没有权限访问此页面。">
      <template #extra>
        <el-button type="primary" @click="goHome">返回首页</el-button>
      </template>
    </el-result>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { ElResult, ElButton } from 'element-plus'

const router = useRouter()

const goHome = () => {
  router.push({ name: 'home' })
}
</script>

<style scoped>
.forbidden-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: calc(100vh - 120px); /* Adjust based on header/footer height */
}
</style>
