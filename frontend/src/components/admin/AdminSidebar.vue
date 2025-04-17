<template>
  <el-menu
    :default-active="activeRoute"
    class="admin-sidebar-menu"
    router
    background-color="#2A3F54"
    text-color="#E7E7E7"
    active-text-color="#FFFFFF"
  >
    <el-menu-item index="/admin/dashboard">
      <el-icon><i-ep-data-analysis /></el-icon>
      <span>仪表盘</span>
    </el-menu-item>
    <el-menu-item index="/admin/users">
      <el-icon><i-ep-user /></el-icon>
      <span>用户管理</span>
    </el-menu-item>
    <el-menu-item index="/admin/movies">
      <el-icon><i-ep-film /></el-icon>
      <span>电影管理</span>
    </el-menu-item>
    <el-menu-item index="/admin/cinemas">
      <el-icon><i-ep-office-building /></el-icon>
      <span>影院管理</span>
    </el-menu-item>
    <el-menu-item index="/admin/screenings">
      <el-icon><i-ep-calendar /></el-icon>
      <span>场次管理</span>
    </el-menu-item>
    <el-menu-item index="/admin/announcements">
      <el-icon><i-ep-data-board /></el-icon>
      <span>公告管理</span>
    </el-menu-item>
    <!-- 可以根据需要添加更多菜单项 -->
  </el-menu>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()

// 计算当前活动的路由，用于菜单高亮
const activeRoute = computed(() => {
  return route.path
})
</script>

<style scoped>
/* Remove width and height here as it's controlled by the layout */
.admin-sidebar-menu {
  border-right: none; /* Remove default border */
  flex-grow: 1; /* Allow menu to fill remaining space */
}

/* Style for active menu item */
.el-menu-item.is-active {
  background-color: #34495e !important; /* Active background color */
}

/* Style for menu item hover */
.el-menu-item:hover {
  background-color: #34495e; /* Hover background color */
}

/* Ensure icons have correct color */
.el-menu-item .el-icon {
  color: #e7e7e7;
}
.el-menu-item.is-active .el-icon {
  color: #ffffff;
}
</style>
