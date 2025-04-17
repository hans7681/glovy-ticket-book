<template>
  <el-container class="cinema-admin-layout admin-layout">
    <!-- Sidebar -->
    <el-aside width="230px" class="cinema-admin-aside">
      <div class="sidebar-logo">
        <!-- Display text only -->
        <span class="logo-text">影院管理后台</span>
      </div>
      <CinemaAdminSidebar />
    </el-aside>

    <el-container class="main-container">
      <!-- Main Content -->
      <el-main class="cinema-admin-main-content">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import CinemaAdminSidebar from '@/components/cinema-admin/CinemaAdminSidebar.vue'
</script>

<style scoped>
.cinema-admin-layout {
  height: 100vh;
  overflow: hidden; /* 防止整体布局出现滚动条 */
  width: 100%;
  position: relative;
}

.cinema-admin-aside {
  background-color: #2a3f54;
  color: #e7e7e7;
  display: flex;
  flex-direction: column;
  border-right: none;
  height: 100vh; /* 确保侧边栏全高 */
  overflow-y: auto; /* 如果导航项过多允许滚动 */
  flex-shrink: 0; /* 防止侧边栏被压缩 */
  position: fixed;
  left: 0;
  top: 0;
  width: 230px; /* 固定宽度与 el-aside 保持一致 */
  z-index: 10;
}

.sidebar-logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #ffffff;
  border-bottom: 1px solid #e0e0e0;
  flex-shrink: 0;
  padding: 0 15px;
}

.logo-text {
  font-size: 18px;
  font-weight: bold;
  color: #333;
}

/* Remove SVG logo style */
/* .logo-svg {
  height: 190%;
  max-width: 200%;
  object-fit: contain;
} */

.main-container {
  background-color: #f7f7f7; /* Light background for main area */
  height: 100vh; /* 确保主容器全高 */
  overflow: hidden; /* 防止主容器自身出现滚动条 */
  display: flex;
  flex-direction: column;
  flex: 1; /* 让主容器占据剩余空间 */
  margin-left: 230px; /* 为固定侧边栏留出空间 */
  width: calc(100% - 230px); /* 减去侧边栏宽度 */
}

.cinema-admin-main-content {
  padding: 0; /* 移除内边距，让子组件自己管理内边距 */
  flex: 1; /* 填充剩余高度 */
  overflow-y: auto; /* 只在内容区域保留滚动功能 */
  overflow-x: hidden; /* 防止水平滚动 */
  position: relative;
  width: 100%;
  height: 100%;
}

/* 确保路由视图不会产生独立滚动 */
:deep(.router-view) {
  overflow: visible !important;
  height: auto !important;
}
</style>
