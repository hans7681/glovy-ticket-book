<template>
  <div class="movie-management-view">
    <div class="page-header">
      <h2>电影信息管理</h2>
    </div>
    <el-card class="tabs-card" shadow="never">
      <el-tabs v-model="activeTab" type="border-card" class="management-tabs">
        <el-tab-pane label="电影管理" name="movies">
          <MovieManagementTab />
        </el-tab-pane>
        <el-tab-pane label="电影类型管理" name="types">
          <MovieTypeManagementTab />
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElTabs, ElTabPane, ElCard } from 'element-plus'
import MovieManagementTab from '@/components/admin/MovieManagementTab.vue' // Assume component path
import MovieTypeManagementTab from '@/components/admin/MovieTypeManagementTab.vue' // Assume component path

const activeTab = ref('movies') // Default tab
</script>

<style scoped>
.movie-management-view {
  padding: 20px;
  height: auto;
  /* 确保视图不设置固定高度，让内容自然流动 */
  width: 100%;
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

.tabs-card {
  margin-bottom: 20px;
  /* 确保卡片不设置固定高度 */
  height: auto;
}

.management-tabs {
  background-color: transparent;
  /* 确保tabs不设置固定高度 */
  height: auto;
  overflow: visible; /* 防止出现不必要的滚动条 */
}

/* 强制覆盖Element Plus的内部滚动行为 */
:deep(.el-tabs__content) {
  padding: 0;
  overflow: visible !important; /* 强制防止内容区出现独立滚动条 */
  height: auto;
}

:deep(.el-tabs__nav-wrap) {
  overflow: visible !important;
}

:deep(.el-tabs__nav-scroll) {
  overflow: visible !important;
}

:deep(.el-card__body) {
  overflow: visible !important;
  padding: 15px;
}

.management-tabs :deep(.el-tabs__item) {
  height: 40px;
  line-height: 40px;
}

.management-tabs :deep(.el-tabs__header) {
  margin-bottom: 15px;
}
</style>
