<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import { fetchMovies, getPublicMovieTypes, getMovieRankings } from '../services/api'
import type { Movie, MovieType } from '../types/movie'
import {
  ElCard,
  ElForm,
  ElFormItem,
  ElRadioGroup,
  ElRadioButton,
  ElSelect,
  ElOption,
  ElPagination,
  ElEmpty,
  ElRow,
  ElCol,
  ElInput,
  ElSkeleton,
  ElSkeletonItem,
  ElImage,
  ElLink,
  ElIcon,
} from 'element-plus'
import { Picture, Search } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

const router = useRouter()

const movies = ref<Movie[]>([])
const movieTypes = ref<MovieType[]>([])
const isLoading = ref(true)
const error = ref<string | null>(null)

// Reactive state for pagination
const pagination = reactive({
  current: 1,
  size: 12, // Changed default page size to 12 (3 rows * 4 cards/row)
  total: 0,
})

// Reactive state for filters
const filters = reactive({
  status: '' as 'NOW_PLAYING' | 'COMING_SOON' | '',
  movieTypeId: '' as number | '',
  title: '',
})

// Ranking List State
interface RankingMovie {
  id: number
  title: string
  posterUrl?: string
  releaseDate?: string // Keep release date if available
  tmdbVoteAverage?: number // For 'hot' list
  tmdbPopularity?: number // For 'upcoming' list
  status?: string // Keep status if needed
}
const hotRankingList = ref<RankingMovie[]>([])
const upcomingRankingList = ref<RankingMovie[]>([])
const loadingRankings = ref(true)

// Fetch Movie Types (Assuming this runs on mount)
const fetchMovieTypes = async () => {
  try {
    const response = await getPublicMovieTypes()
    movieTypes.value = response.data || []
  } catch (error) {
    console.error('Failed to fetch movie types:', error)
    ElMessage.error('加载电影类型失败')
  }
}

// Fetch Movies and Rankings
const fetchData = async () => {
  isLoading.value = true
  error.value = null
  try {
    // Fetch movie list
    const movieParams = {
      current: pagination.current,
      size: pagination.size,
      ...(filters.status && { status: filters.status }),
      ...(filters.movieTypeId && { movieTypeId: filters.movieTypeId }),
      ...(filters.title && { title: filters.title }),
      sortField: 'status', // 添加排序字段
      sortOrder: 'asc', // 升序排列（NOW_PLAYING会在COMING_SOON前面）
    }

    // Now the 'params' object should match the expected type for fetchMovies
    const movieResPromise = fetchMovies(movieParams)

    // Fetch rankings in parallel
    const hotRankingPromise = getMovieRankings('hot', 10)
    const upcomingRankingPromise = getMovieRankings('upcoming', 10)

    // Await all promises
    const [movieRes, hotRes, upcomingRes] = await Promise.all([
      movieResPromise,
      hotRankingPromise,
      upcomingRankingPromise,
    ])

    // Process movie list response
    movies.value = movieRes.data.records || []
    pagination.total = movieRes.data.total || 0
    if (movieRes.data.current) {
      pagination.current = movieRes.data.current
    }

    // Process rankings response
    hotRankingList.value = hotRes.data as RankingMovie[]
    upcomingRankingList.value = upcomingRes.data as RankingMovie[]
  } catch (err) {
    console.error('Failed to load movies:', err)
    error.value = '无法加载电影列表，请稍后重试。'
    movies.value = []
    pagination.total = 0
  } finally {
    isLoading.value = false
    loadingRankings.value = false
  }
}

// Handle page size change from ElPagination
const handleSizeChange = (newSize: number) => {
  pagination.size = newSize
  pagination.current = 1 // Reset to first page when size changes
  fetchData()
}

// Handle current page change from ElPagination
const handleCurrentChange = (newPage: number) => {
  pagination.current = newPage
  fetchData()
}

// Handle filter changes (called by @change on filter components)
const handleFilterChange = () => {
  pagination.current = 1 // Reset to first page on filter change
  fetchData()
}

// Navigate to Movie Detail Page
const goToDetail = (id: number | string | undefined) => {
  if (id === undefined) {
    console.error('Attempted to navigate to detail page with undefined id')
    return
  }
  // Use the correct route name as defined in router/index.ts
  router.push({ name: 'movie-detail', params: { id: String(id) } })
}

// Load initial data when component is mounted
onMounted(() => {
  fetchMovieTypes() // Fetch types for the filter
  fetchData() // Fetch initial movie list
})
</script>

<template>
  <div class="movie-list-view container">
    <!-- 筛选区域 -->
    <el-card class="filter-card" shadow="never">
      <el-form :inline="true" :model="filters" @submit.prevent="handleFilterChange">
        <el-form-item label="状态">
          <el-radio-group v-model="filters.status" @change="handleFilterChange">
            <el-radio-button value="">全部</el-radio-button>
            <el-radio-button value="NOW_PLAYING">正在热映</el-radio-button>
            <el-radio-button value="COMING_SOON">即将上映</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="类型">
          <el-select
            v-model="filters.movieTypeId"
            placeholder="全部类型"
            clearable
            @change="handleFilterChange"
            style="width: 200px"
          >
            <el-option
              v-for="type in movieTypes"
              :key="type.id"
              :label="type.name"
              :value="type.id"
            ></el-option>
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-input
            v-model="filters.title"
            placeholder="搜索电影标题"
            clearable
            @change="handleFilterChange"
            style="width: 200px"
          >
            <template #append>
              <el-button @click="handleFilterChange">
                <el-icon><Search /></el-icon>
              </el-button>
            </template>
          </el-input>
        </el-form-item>
      </el-form>
    </el-card>

    <el-row :gutter="20">
      <!-- 左侧电影列表 -->
      <el-col :xs="24" :sm="24" :md="18" :lg="19" :xl="19">
        <el-skeleton :loading="isLoading" animated :rows="5">
          <!-- Adjust skeleton rows -->
          <template #template>
            <el-row :gutter="16">
              <el-col
                v-for="i in pagination.size"
                :key="i"
                :xs="12"
                :sm="8"
                :md="6"
                :lg="6"
                :xl="6"
                class="movie-card-col"
              >
                <el-card :body-style="{ padding: '0px' }" shadow="hover">
                  <el-skeleton-item variant="image" style="width: 100%; height: 280px" />
                  <div style="padding: 14px">
                    <el-skeleton-item variant="p" style="width: 70%; margin-bottom: 8px" />
                    <el-skeleton-item variant="text" style="width: 50%" />
                  </div>
                </el-card>
              </el-col>
            </el-row>
          </template>
          <template #default>
            <el-row :gutter="16" v-if="movies.length > 0">
              <el-col
                v-for="movie in movies"
                :key="movie.id"
                :xs="12"
                :sm="8"
                :md="6"
                :lg="6"
                :xl="6"
                class="movie-card-col"
              >
                <el-card
                  :body-style="{ padding: '0px' }"
                  shadow="hover"
                  @click="goToDetail(movie.id)"
                  class="movie-card"
                >
                  <el-image
                    :src="movie.posterUrl || '/img/placeholder.jpg'"
                    fit="cover"
                    class="movie-poster"
                    lazy
                  >
                    <template #placeholder>
                      <div class="image-slot">Loading...</div>
                    </template>
                    <template #error>
                      <div class="image-slot">
                        <el-icon><Picture /></el-icon>
                        <!-- Use appropriate icon -->
                      </div>
                    </template>
                  </el-image>
                  <div style="padding: 14px">
                    <span class="movie-title" :title="movie.title">{{ movie.title }}</span>
                    <div class="bottom">
                      <time class="time">上映: {{ movie.releaseDate || '未知' }}</time>
                      <span
                        class="rating"
                        v-if="movie.status === 'NOW_PLAYING' && movie.tmdbVoteAverage"
                      >
                        评分: {{ movie.tmdbVoteAverage.toFixed(1) }}
                      </span>
                    </div>
                  </div>
                </el-card>
              </el-col>
            </el-row>
            <el-empty v-else description="没有找到匹配的电影"></el-empty>

            <!-- 分页 -->
            <el-pagination
              background
              layout="prev, pager, next, jumper, ->, total, sizes"
              v-if="pagination.total > 0"
              :total="pagination.total"
              v-model:current-page="pagination.current"
              v-model:page-size="pagination.size"
              :page-sizes="[12, 24, 36, 48]"
              @size-change="handleSizeChange"
              @current-change="handleCurrentChange"
              style="margin-top: 20px; justify-content: center"
            />
          </template>
        </el-skeleton>
      </el-col>

      <!-- 右侧排行榜 -->
      <el-col :xs="24" :sm="24" :md="6" :lg="5" :xl="5" class="ranking-col">
        <el-skeleton :loading="loadingRankings" animated>
          <template #template>
            <el-card shadow="never" style="margin-bottom: 20px">
              <el-skeleton-item variant="h3" style="width: 50%; margin-bottom: 15px" />
              <div v-for="i in 5" :key="'sk-hot-' + i" class="ranking-item-skeleton">
                <el-skeleton-item variant="text" style="width: 20px" />
                <el-skeleton-item variant="image" style="width: 45px; height: 65px" />
                <div style="flex: 1; margin-left: 10px">
                  <el-skeleton-item variant="p" style="width: 80%; margin-bottom: 5px" />
                  <el-skeleton-item variant="text" style="width: 40%" />
                </div>
              </div>
            </el-card>
            <el-card shadow="never">
              <el-skeleton-item variant="h3" style="width: 50%; margin-bottom: 15px" />
              <div v-for="i in 5" :key="'sk-up-' + i" class="ranking-item-skeleton">
                <el-skeleton-item variant="text" style="width: 20px" />
                <el-skeleton-item variant="image" style="width: 45px; height: 65px" />
                <div style="flex: 1; margin-left: 10px">
                  <el-skeleton-item variant="p" style="width: 80%; margin-bottom: 5px" />
                  <el-skeleton-item variant="text" style="width: 40%" />
                </div>
              </div>
            </el-card>
          </template>
          <template #default>
            <!-- 热映口碑榜 -->
            <el-card shadow="never" class="ranking-card">
              <template #header>
                <div class="card-header">
                  <span>🔥 热映口碑榜</span>
                </div>
              </template>
              <div v-if="hotRankingList.length > 0">
                <div
                  v-for="(movie, index) in hotRankingList"
                  :key="movie.id"
                  class="ranking-item"
                  @click="goToDetail(movie.id)"
                >
                  <span class="ranking-index" :class="{ 'top-3': index < 3 }">{{ index + 1 }}</span>
                  <el-image
                    :src="movie.posterUrl || '/img/placeholder.jpg'"
                    fit="cover"
                    class="ranking-poster"
                    lazy
                  />
                  <div class="ranking-info">
                    <el-link
                      type="primary"
                      class="ranking-title"
                      :title="movie.title"
                      @click.stop="goToDetail(movie.id)"
                      >{{ movie.title }}</el-link
                    >
                    <span class="ranking-metric"
                      >评分: {{ movie.tmdbVoteAverage?.toFixed(1) ?? 'N/A' }}</span
                    >
                  </div>
                </div>
              </div>
              <el-empty v-else description="暂无数据" :image-size="50" />
            </el-card>

            <!-- 最受期待榜 -->
            <el-card shadow="never" class="ranking-card" style="margin-top: 20px">
              <template #header>
                <div class="card-header">
                  <span>🌟 最受期待榜</span>
                </div>
              </template>
              <div v-if="upcomingRankingList.length > 0">
                <div
                  v-for="(movie, index) in upcomingRankingList"
                  :key="movie.id"
                  class="ranking-item"
                  @click="goToDetail(movie.id)"
                >
                  <span class="ranking-index" :class="{ 'top-3': index < 3 }">{{ index + 1 }}</span>
                  <el-image
                    :src="movie.posterUrl || '/img/placeholder.jpg'"
                    fit="cover"
                    class="ranking-poster"
                    lazy
                  />
                  <div class="ranking-info">
                    <el-link
                      type="primary"
                      class="ranking-title"
                      :title="movie.title"
                      @click.stop="goToDetail(movie.id)"
                      >{{ movie.title }}</el-link
                    >
                    <span class="ranking-metric"
                      >热度: {{ movie.tmdbPopularity?.toFixed(0) ?? 'N/A' }}</span
                    >
                  </div>
                </div>
              </div>
              <el-empty v-else description="暂无数据" :image-size="50" />
            </el-card>
          </template>
        </el-skeleton>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
.container {
  max-width: 1300px; /* Limit overall container width */
  margin: 0 auto; /* Center the container */
}
.movie-list-view {
  padding: 20px;
}
.filter-card {
  margin-bottom: 20px;
  padding: 10px 20px; /* Adjust padding */
}
.filter-card .el-form-item {
  margin-bottom: 0; /* Remove bottom margin for inline form */
}

.movie-card-col {
  margin-bottom: 16px;
}

.movie-card {
  cursor: pointer;
  transition:
    transform 0.2s ease-in-out,
    box-shadow 0.2s ease-in-out;
  height: 100%; /* Make cards in a row equal height if needed */
  display: flex;
  flex-direction: column;
}

.movie-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.movie-poster {
  width: 100%;
  /* Adjust height based on desired aspect ratio, e.g., 3:2 */
  aspect-ratio: 2 / 3; /* Use modern CSS for aspect ratio */
  display: block;
  object-fit: cover; /* Ensure image covers the area */
}
.image-slot {
  display: flex;
  justify-content: center;
  align-items: center;
  width: 100%;
  height: 100%; /* Make slot fill the space */
  position: absolute; /* Position correctly within el-image */
  top: 0;
  left: 0;
  background: #f5f7fa;
  color: var(--el-text-color-secondary);
  font-size: 14px;
}
.image-slot .el-icon {
  font-size: 30px;
}

.movie-title {
  font-size: 15px;
  font-weight: 500;
  color: #303133;
  display: block;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-bottom: 8px;
  line-height: 1.3;
}

.time {
  font-size: 12px;
  color: #999;
}
.rating {
  font-size: 12px;
  color: #ffac2d; /* Rating color */
  font-weight: bold;
}

.bottom {
  margin-top: 10px;
  line-height: 1;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.ranking-col {
  /* Styles for the ranking column itself */
}

.ranking-card .card-header span {
  font-weight: bold;
  font-size: 16px;
}
.ranking-item {
  display: flex;
  align-items: center;
  margin-bottom: 12px; /* Slightly reduce margin */
  cursor: pointer;
  transition: background-color 0.2s;
  padding: 5px 0; /* Add some vertical padding */
}
.ranking-item:hover {
  background-color: #f8f9fa; /* Lighter hover */
}

.ranking-item-skeleton {
  display: flex;
  align-items: center;
  margin-bottom: 12px;
  padding: 5px 0;
}

.ranking-index {
  font-size: 16px;
  font-weight: bold;
  color: #909399;
  width: 25px; /* Adjust width */
  text-align: center;
  margin-right: 10px;
  flex-shrink: 0;
}
.ranking-index.top-3 {
  color: #e6a23c; /* Gold/Orange for top 3 */
}

.ranking-poster {
  width: 45px;
  height: 65px;
  margin-right: 10px;
  flex-shrink: 0;
  border-radius: 3px;
  object-fit: cover;
}
.ranking-info {
  display: flex;
  flex-direction: column;
  overflow: hidden;
  flex-grow: 1;
}
.ranking-title {
  font-size: 14px;
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  /* Reset link styles */
  padding: 0;
  height: auto;
  line-height: 1.4;
}
.ranking-title :deep(.el-link__inner) {
  text-align: left;
  display: inline; /* Prevent block behavior */
}
.ranking-metric {
  font-size: 12px;
  color: #909399;
}

/* Responsive adjustments */
@media (max-width: 992px) {
  .ranking-col {
    margin-top: 20px;
  }
}
</style>
