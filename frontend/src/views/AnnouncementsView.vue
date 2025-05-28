<template>
  <div class="announcements-container">
    <el-card class="announcements-card">
      <template #header>
        <div class="announcements-header">
          <h1>公告信息</h1>
        </div>
      </template>

      <el-skeleton :rows="5" animated v-if="loading" />

      <div v-else-if="announcements.length === 0" class="empty-data">
        <el-empty description="暂无公告信息" />
      </div>

      <div v-else class="announcements-list">
        <el-timeline>
          <el-timeline-item
            v-for="announcement in announcements"
            :key="announcement.id"
            :timestamp="formatDate(announcement.publishTime || announcement.createTime)"
            placement="top"
            type="primary"
          >
            <el-card class="announcement-card">
              <h3>{{ announcement.title }}</h3>
              <div class="announcement-content">
                {{ announcement.content }}
              </div>
            </el-card>
          </el-timeline-item>
        </el-timeline>
      </div>

      <div class="pagination-container" v-if="!loading && total > 0">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[5, 10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { listPublishedAnnouncements } from '@/services/api'
import type { Announcement } from '@/types/announcement'
import { format } from 'date-fns'

// 分页相关参数
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 数据和加载状态
const announcements = ref<Announcement[]>([])
const loading = ref(true)

// 获取公告列表数据
const fetchAnnouncements = async () => {
  loading.value = true
  try {
    const response = await listPublishedAnnouncements({
      current: currentPage.value,
      size: pageSize.value,
    })

    announcements.value = response.data.records
    total.value = response.data.total
  } catch (error) {
    console.error('获取公告列表失败:', error)
    ElMessage.error('获取公告列表失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

// 处理分页变化
const handleCurrentChange = (val: number) => {
  currentPage.value = val
  fetchAnnouncements()
}

const handleSizeChange = (val: number) => {
  pageSize.value = val
  currentPage.value = 1
  fetchAnnouncements()
}

// 格式化日期
const formatDate = (dateString: string) => {
  try {
    return format(new Date(dateString), 'yyyy-MM-dd HH:mm')
  } catch {
    return dateString
  }
}

// 组件挂载时加载数据
onMounted(() => {
  fetchAnnouncements()
})
</script>

<style scoped>
.announcements-container {
  max-width: 1000px;
  margin: 20px auto;
  padding: 0 20px;
}

.announcements-card {
  margin-bottom: 20px;
}

.announcements-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.announcements-header h1 {
  margin: 0;
  font-size: 1.5rem;
  color: #303133;
}

.empty-data {
  padding: 40px 0;
}

.announcement-card {
  margin-bottom: 10px;
}

.announcement-card h3 {
  margin-top: 0;
  margin-bottom: 10px;
  color: #409eff;
}

.announcement-content {
  white-space: pre-line;
  line-height: 1.6;
  color: #606266;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}
</style>
