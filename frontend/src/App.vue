<script setup lang="ts">
import { RouterView } from 'vue-router'
import AppHeader from './components/layout/AppHeader.vue'
import AppFooter from './components/layout/AppFooter.vue'
</script>

<template>
  <div class="app">
    <AppHeader />

    <main class="main-content">
      <RouterView />
    </main>

    <AppFooter />
  </div>
</template>

<style>
/* 全局CSS重置和基础样式 */
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif;
  color: #333;
  line-height: 1.6;
  background-color: #f5f5f5;
  font-size: 16px;
  min-height: 100vh;
}

.app {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  width: 100%;
}

.main-content {
  flex: 1;
  /* Apply width/padding only if it doesn't contain the admin layout */
  /* We target the direct child of .main-content */
  /* This selector might need adjustment if your structure changes */
}

/* Apply max-width and padding only when NOT inside admin layout */
.main-content > :not(.admin-layout) {
  max-width: 1200px;
  width: 100%;
  margin: 0 auto;
  padding: 2rem 1rem;
}
/* Ensure admin layout itself takes full width/height if needed */
.main-content > .admin-layout {
  width: 100%;
  height: 100%; /* Or adjust as needed */
  /* Reset padding and margin applied by .main-content or others if necessary */
  padding: 0;
  margin: 0;
  max-width: none; /* Override max-width */
}

/* 响应式设计 */
@media (max-width: 768px) {
  body {
    font-size: 14px;
  }

  .main-content {
    padding: 1rem;
  }
}

/* 链接通用样式 */
a {
  color: #e50914;
  text-decoration: none;
}

a:hover {
  text-decoration: underline;
}

/* 按钮通用样式 */
button {
  cursor: pointer;
}

/* 表单元素通用样式 */
input,
textarea,
select,
button {
  font-family: inherit;
  font-size: inherit;
}
</style>
