<template>
  <el-menu
    :default-active="activeRoute"
    class="cinema-admin-sidebar-menu"
    router
    background-color="#2A3F54"
    text-color="#E7E7E7"
    active-text-color="#FFFFFF"
  >
    <!-- Default active item -->
    <!-- Keep consistent dark theme -->
    <el-menu-item index="/cinema-admin/my-cinema">
      <el-icon><i-ep-school /></el-icon>
      <!-- Example icon -->
      <span>我的影院</span>
    </el-menu-item>
    <el-menu-item index="/cinema-admin/rooms">
      <el-icon><i-ep-grid /></el-icon>
      <!-- Example icon -->
      <span>影厅管理</span>
    </el-menu-item>
    <el-menu-item index="/cinema-admin/screenings">
      <el-icon><i-ep-calendar /></el-icon>
      <!-- Example icon -->
      <span>场次管理</span>
    </el-menu-item>
    <el-menu-item index="/cinema-admin/orders">
      <el-icon><i-ep-list /></el-icon>
      <!-- Example icon -->
      <span>订单管理</span>
    </el-menu-item>
    <!-- Add more items if needed -->
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
.cinema-admin-sidebar-menu {
  border-right: none;
  flex-grow: 1;
}

.el-menu-item.is-active {
  background-color: #34495e !important;
}

.el-menu-item:hover {
  background-color: #34495e;
}

.el-menu-item .el-icon {
  color: #e7e7e7;
}

.el-menu-item.is-active .el-icon {
  color: #ffffff;
}
</style>
