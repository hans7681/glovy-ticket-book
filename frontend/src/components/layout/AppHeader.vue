<script setup lang="ts">
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import {
  ElHeader,
  ElMenu,
  ElMenuItem,
  ElDropdown,
  ElDropdownMenu,
  ElDropdownItem,
  ElIcon,
  ElMessage,
  ElMessageBox,
} from 'element-plus'
import { ArrowDown, User, List, Setting, SwitchButton } from '@element-plus/icons-vue'

const authStore = useAuthStore()
const router = useRouter()
const route = useRoute()

// 计算当前活动路由，用于菜单高亮
const activeIndex = computed(() => {
  // 获取当前路径
  const path = route.path

  // 检查是否在电影相关页面
  if (path.startsWith('/movies') || path === '/movie') {
    return '/movies'
  }

  // 检查是否在影院相关页面
  if (path.startsWith('/cinemas') || path === '/cinema') {
    return '/cinemas'
  }

  // 默认返回完整路径
  return path
})

const isLoggedIn = computed(() => authStore.isLoggedIn)
const userNickname = computed(() => authStore.user?.nickname || authStore.user?.username)
const userRole = computed(() => authStore.user?.role)

const goTo = (path: string) => {
  router.push(path)
}

const handleLogout = async () => {
  try {
    await ElMessageBox.confirm('确定要退出登录吗?', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    // No need to await the logout action itself if it's synchronous
    authStore.logout() // Call store logout action (which handles redirect)
    ElMessage.success('已成功退出登录')
    // Redirect is handled within authStore.logout()
    // router.push('/')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('退出登录时发生错误')
      console.error('Logout failed:', error)
    } else {
      ElMessage.info('已取消退出登录')
    }
  }
}
</script>

<template>
  <el-header class="app-header">
    <!-- Use ElRow/ElCol or similar for layout if needed, or keep simple flex -->
    <div class="header-content">
      <div class="logo">
        <!-- Replace text with SVG logo -->
        <router-link to="/">
          <img src="/videos/logo.svg" alt="Glovy 光维娱乐 Logo" class="header-logo-svg" />
        </router-link>
      </div>

      <!-- Navigation Menu using Element Plus -->
      <el-menu
        mode="horizontal"
        :ellipsis="false"
        router
        :default-active="activeIndex"
        class="header-menu"
      >
        <el-menu-item index="/">首页</el-menu-item>
        <el-menu-item index="/movies">电影</el-menu-item>
        <el-menu-item index="/cinemas">影院</el-menu-item>
        <el-menu-item index="/announcements">公告</el-menu-item>

        <!-- Role-based Admin Links -->
        <el-menu-item v-if="isLoggedIn && userRole === 'SYSTEM_ADMIN'" index="/admin">
          系统管理
        </el-menu-item>
        <el-menu-item v-if="isLoggedIn && userRole === 'CINEMA_ADMIN'" index="/cinema-admin">
          影院管理
        </el-menu-item>

        <div class="flex-grow" />

        <!-- Auth Links -->
        <template v-if="!isLoggedIn">
          <el-menu-item index="/login">登录</el-menu-item>
          <el-menu-item index="/register">注册</el-menu-item>
        </template>

        <template v-if="isLoggedIn">
          <el-dropdown class="user-dropdown">
            <span class="el-dropdown-link">
              <el-icon class="mr-1"><User /></el-icon>
              {{ userNickname || '用户' }}
              <el-icon class="el-icon--right"><arrow-down /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="goTo('/orders')">
                  <el-icon><List /></el-icon>我的订单
                </el-dropdown-item>
                <el-dropdown-item @click="goTo('/profile')">
                  <el-icon><Setting /></el-icon>个人中心
                </el-dropdown-item>
                <el-dropdown-item divided @click="handleLogout">
                  <el-icon><SwitchButton /></el-icon>退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
      </el-menu>
    </div>
  </el-header>
</template>

<style scoped>
.app-header {
  --el-header-padding: 0 20px; /* Adjust padding if needed */
  --el-header-height: 60px; /* Standard Element Plus header height */
  border-bottom: 1px solid var(--el-border-color-light);
  background-color: #fff; /* Or your desired background */
}

.header-content {
  display: flex;
  align-items: center;
  height: 100%;
  max-width: 1200px; /* Or your preferred max width */
  margin: 0 auto;
}

.logo {
  margin-right: 30px;
}

.logo a {
  /* Remove text-specific styles */
  /* font-size: 1.3rem; */
  /* font-weight: bold; */
  /* color: var(--el-text-color-primary); */
  /* text-decoration: none; */
  display: flex; /* Make link a flex container for the image */
  align-items: center;
  height: 100%; /* Ensure link fills logo area height */
}

/* Add styles for the header logo SVG */
.header-logo-svg {
  height: 55px; /* Increase height to match sidebar logo prominence */
  /* Remove or increase max-width to let height control size */
  /* max-width: 150px; */
  object-fit: contain;
}

.header-menu {
  flex-grow: 1;
  border-bottom: none; /* Remove default border from menu */
}

.flex-grow {
  flex-grow: 1;
}

/* Style the dropdown trigger to look like a menu item */
.user-dropdown {
  height: var(--el-menu-item-height);
  line-height: var(--el-menu-item-height);
  padding: 0 15px; /* Adjust padding */
  cursor: pointer;
}

.el-dropdown-link {
  display: flex;
  align-items: center;
  color: var(--el-text-color-regular);
}

.el-dropdown-link:hover {
  color: var(--el-color-primary);
}

.el-dropdown-link .el-icon {
  margin-left: 5px;
}

.el-dropdown-menu__item .el-icon {
  margin-right: 5px;
}

.mr-1 {
  margin-right: 5px;
}

/* Ensure dropdown aligns well when menu collapses */
@media (max-width: 768px) {
  /* Element Plus menu handles collapse, but dropdown might need adjustments */
  .user-dropdown {
    padding: 0 10px; /* Adjust padding for smaller screens */
  }
  .header-content {
    padding: 0 10px; /* Reduce padding on mobile */
  }
}
</style>
