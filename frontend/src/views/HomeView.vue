<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { RouterLink } from 'vue-router'
import { fetchMovies } from '../services/api' // Adjusted import path
import type { Movie } from '../types/movie' // Adjusted import path
import { useAuthStore } from '@/stores/auth'

// 获取认证状态
const authStore = useAuthStore()
const isLoggedIn = computed(() => authStore.isLoggedIn)

const popularMovies = ref<Movie[]>([])
const upcomingMovies = ref<Movie[]>([])
const isLoadingPopular = ref(true)
const isLoadingUpcoming = ref(true)
const errorPopular = ref<string | null>(null)
const errorUpcoming = ref<string | null>(null)

// Function to construct full image URL if posterUrl is just a path
// Adjust this based on how your backend provides the URL or if TMDB base URL is needed
const getImageUrl = (path: string | undefined) => {
  if (!path) {
    // Return a placeholder image URL if path is missing
    return 'https://via.placeholder.com/200x300.png?text=No+Image'
  }
  // Assuming path is a full URL from your backend
  // If it's a TMDB path, prepend the base URL: `https://image.tmdb.org/t/p/w500${path}`
  return path
}

async function loadPopularMovies() {
  isLoadingPopular.value = true
  errorPopular.value = null
  try {
    const response = await fetchMovies({
      status: 'NOW_PLAYING',
      current: 1,
      size: 5,
      // sort: 'tmdbPopularity,desc' // Uncomment or adjust if your API supports sorting
    })
    popularMovies.value = response.data.records // Access records from Page structure
  } catch (error) {
    console.error('Failed to load popular movies:', error)
    errorPopular.value = '无法加载热门电影，请稍后重试。'
  } finally {
    isLoadingPopular.value = false
  }
}

async function loadUpcomingMovies() {
  isLoadingUpcoming.value = true
  errorUpcoming.value = null
  try {
    const response = await fetchMovies({
      status: 'COMING_SOON',
      current: 1,
      size: 5,
      // sort: 'releaseDate,asc' // Uncomment or adjust if your API supports sorting
    })
    upcomingMovies.value = response.data.records // Access records from Page structure
  } catch (error) {
    console.error('Failed to load upcoming movies:', error)
    errorUpcoming.value = '无法加载即将上映电影，请稍后重试。'
  } finally {
    isLoadingUpcoming.value = false
  }
}

onMounted(() => {
  loadPopularMovies()
  loadUpcomingMovies()
})
</script>

<template>
  <div class="home">
    <!-- 英雄区域/视频背景 -->
    <section class="hero">
      <!-- 视频背景 -->
      <video class="hero-video" autoplay loop muted playsinline>
        <source src="/videos/hero-bg.mp4" type="video/mp4" />
        <!-- 如果视频无法播放，回退到原有背景 -->
      </video>

      <!-- 半透明遮罩层：单独的元素，而非伪元素 -->
      <div class="hero-overlay"></div>

      <!-- 内容浮层 -->
      <div class="hero-content">
        <h1>发现精彩电影世界</h1>
        <p>最新、最热门的电影尽在这里</p>
        <div class="hero-buttons">
          <RouterLink to="/movies" class="btn btn-primary">浏览电影</RouterLink>
          <RouterLink v-if="!isLoggedIn" to="/register" class="btn btn-secondary"
            >注册账号</RouterLink
          >
          <RouterLink v-else to="/cinemas" class="btn btn-secondary">浏览影院</RouterLink>
        </div>
      </div>
    </section>

    <!-- 热门电影区域 -->
    <section class="featured-section">
      <h2 class="section-title">热门电影</h2>
      <!-- Loading State -->
      <div v-if="isLoadingPopular" class="movie-grid">
        <div class="movie-card placeholder-card" v-for="i in 5" :key="`pop-ph-${i}`">
          <div class="movie-poster placeholder"></div>
          <div class="movie-info">
            <h3 class="movie-title placeholder-text"></h3>
            <p class="movie-meta placeholder-text"></p>
          </div>
        </div>
      </div>
      <!-- Error State -->
      <div v-else-if="errorPopular" class="error-message">
        {{ errorPopular }}
      </div>
      <!-- Success State -->
      <div v-else class="movie-grid">
        <RouterLink
          v-for="movie in popularMovies"
          :key="movie.id"
          :to="'/movies/' + movie.id"
          class="movie-card-link"
        >
          <div class="movie-card">
            <img :src="getImageUrl(movie.posterUrl)" :alt="movie.title" class="movie-poster-img" />
            <div class="movie-info">
              <h3 class="movie-title">{{ movie.title }}</h3>
              <p class="movie-meta">{{ movie.releaseDate }}</p>
              <!-- Add more meta info like rating if available -->
            </div>
          </div>
        </RouterLink>
        <!-- Message if no movies found -->
        <p v-if="!popularMovies.length && !isLoadingPopular && !errorPopular">暂无热门电影。</p>
      </div>
      <div v-if="!isLoadingPopular && !errorPopular && popularMovies.length" class="view-more">
        <RouterLink to="/movies" class="btn btn-outline">查看更多</RouterLink>
      </div>
    </section>

    <!-- 即将上映区域 -->
    <section class="featured-section">
      <h2 class="section-title">即将上映</h2>
      <!-- Loading State -->
      <div v-if="isLoadingUpcoming" class="movie-grid">
        <div class="movie-card placeholder-card" v-for="i in 5" :key="`up-ph-${i}`">
          <div class="movie-poster placeholder"></div>
          <div class="movie-info">
            <h3 class="movie-title placeholder-text"></h3>
            <p class="movie-meta placeholder-text"></p>
          </div>
        </div>
      </div>
      <!-- Error State -->
      <div v-else-if="errorUpcoming" class="error-message">
        {{ errorUpcoming }}
      </div>
      <!-- Success State -->
      <div v-else class="movie-grid">
        <RouterLink
          v-for="movie in upcomingMovies"
          :key="movie.id"
          :to="'/movies/' + movie.id"
          class="movie-card-link"
        >
          <div class="movie-card">
            <img :src="getImageUrl(movie.posterUrl)" :alt="movie.title" class="movie-poster-img" />
            <div class="movie-info">
              <h3 class="movie-title">{{ movie.title }}</h3>
              <p class="movie-meta">上映日期: {{ movie.releaseDate }}</p>
            </div>
          </div>
        </RouterLink>
        <!-- Message if no movies found -->
        <p v-if="!upcomingMovies.length && !isLoadingUpcoming && !errorUpcoming">
          暂无即将上映电影。
        </p>
      </div>
      <div v-if="!isLoadingUpcoming && !errorUpcoming && upcomingMovies.length" class="view-more">
        <RouterLink to="/movies" class="btn btn-outline">查看更多</RouterLink>
      </div>
    </section>
  </div>
</template>

<style scoped>
.home {
  width: 100%;
}

/* 英雄区域样式 */
.hero {
  position: relative;
  height: 500px;
  display: flex;
  align-items: center;
  justify-content: center;
  text-align: center;
  color: white;
  margin-bottom: 3rem;
  border-radius: 8px;
  overflow: hidden;
  width: 100%;
}

/* 视频背景样式 */
.hero-video {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 99%;
  object-fit: cover; /* 确保视频覆盖整个区域 */
  z-index: 0; /* 放在底层 */
}

/* 使用具体元素而非伪元素实现遮罩 */
.hero-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: linear-gradient(
    to bottom,
    rgba(0, 0, 0, 0.6),
    rgba(0, 0, 0, 0.4)
  ); /* 渐变效果增强视觉体验 */
  z-index: 1;
}

.hero-content {
  max-width: 800px;
  padding: 0 2rem;
  position: relative;
  z-index: 2; /* 确保内容在遮罩上方 */
}

.hero h1 {
  font-size: 3rem;
  margin-bottom: 1rem;
  font-weight: 700;
  text-shadow: 0 2px 8px rgba(0, 0, 0, 0.7); /* 增强文字阴影效果 */
  letter-spacing: 1px; /* 增加字间距提高可读性 */
}

.hero p {
  font-size: 1.5rem;
  margin-bottom: 2rem;
  text-shadow: 0 2px 8px rgba(0, 0, 0, 0.7); /* 增强文字阴影效果 */
}

.hero-buttons {
  display: flex;
  justify-content: center;
  gap: 1rem;
}

.btn {
  padding: 0.75rem 1.5rem;
  border-radius: 4px;
  font-weight: 600;
  text-align: center;
  transition: all 0.3s;
  text-decoration: none;
}

.btn-primary {
  background-color: #e50914;
  color: white;
}

.btn-primary:hover {
  background-color: #f40612;
  transform: translateY(-2px);
}

.btn-secondary {
  background-color: rgba(255, 255, 255, 0.2);
  color: white;
  border: 2px solid white;
}

.btn-secondary:hover {
  background-color: rgba(255, 255, 255, 0.3);
  transform: translateY(-2px);
}

.btn-outline {
  border: 2px solid #e50914;
  color: #e50914;
  background: transparent;
}

.btn-outline:hover {
  background-color: #e50914;
  color: white;
}

/* 电影区域样式 */
.featured-section {
  margin-bottom: 3rem;
  width: 100%;
}

.section-title {
  font-size: 2rem;
  margin-bottom: 1.5rem;
  font-weight: 700;
  color: #333;
}

.movie-grid {
  display: grid;
  /* Adjust grid columns for 5 items per row, considering gaps */
  grid-template-columns: repeat(5, 1fr);
  gap: 1.5rem; /* Adjust gap as needed */
  margin-bottom: 2rem;
  width: 100%;
}

.movie-card-link {
  text-decoration: none;
  color: inherit; /* Prevent default link color */
  display: block; /* Ensure link takes up card space */
}

.movie-card {
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
  transition:
    transform 0.3s,
    box-shadow 0.3s;
  background-color: white;
  height: 100%; /* Make card fill grid item height */
  display: flex;
  flex-direction: column;
}

.movie-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 8px 16px rgba(0, 0, 0, 0.2);
}

.movie-poster-img {
  width: 100%;
  height: 300px; /* Adjust height as needed */
  object-fit: cover; /* Cover the area, might crop */
  display: block; /* Remove extra space below image */
}

.movie-info {
  padding: 1rem;
  flex-grow: 1; /* Allow info to take remaining space */
}

.movie-title {
  font-size: 1.1rem;
  margin-bottom: 0.5rem;
  font-weight: 600; /* Make title slightly bolder */
  /* Limit title to 2 lines with ellipsis */
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
  line-height: 1.3; /* Adjust line height */
  min-height: 2.6em; /* Ensure space for 2 lines */
}

.movie-meta {
  color: #777;
  font-size: 0.9rem;
}

.view-more {
  text-align: center;
}

.error-message {
  color: #c62828;
  background-color: #ffebee;
  padding: 1rem;
  border-radius: 4px;
  text-align: center;
}

/* Placeholder Styles */
.placeholder-card {
  background-color: white;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
}

.placeholder {
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: loading 1.5s infinite;
}

.movie-poster.placeholder {
  height: 300px; /* Match image height */
  width: 100%;
}

.placeholder-text {
  height: 1rem;
  width: 100%;
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: loading 1.5s infinite;
  border-radius: 4px;
  margin: 0.5rem 0;
}

.movie-title.placeholder-text {
  height: 1.2rem;
  width: 80%;
}

@keyframes loading {
  0% {
    background-position: 200% 0;
  }
  100% {
    background-position: -200% 0;
  }
}

/* Responsive adjustments for movie grid */
@media (max-width: 1200px) {
  .movie-grid {
    grid-template-columns: repeat(4, 1fr);
  }
}

@media (max-width: 992px) {
  .movie-grid {
    grid-template-columns: repeat(3, 1fr);
  }
  .movie-poster-img,
  .movie-poster.placeholder {
    height: 250px; /* Adjust height for smaller screens */
  }
}

@media (max-width: 768px) {
  .hero {
    height: 400px;
  }
  .hero h1 {
    font-size: 2rem;
  }
  .hero p {
    font-size: 1.2rem;
  }
  .movie-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 1rem;
  }
  .movie-poster-img,
  .movie-poster.placeholder {
    height: 220px;
  }
}

@media (max-width: 480px) {
  .hero-buttons {
    flex-direction: column;
  }
  .movie-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 1rem;
  }
  .movie-poster-img,
  .movie-poster.placeholder {
    height: 180px;
  }
  .movie-title {
    font-size: 1rem;
    min-height: 2.4em; /* Adjust for smaller font */
  }
}
</style>
