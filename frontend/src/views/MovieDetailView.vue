<script setup lang="ts">
import { ref, onMounted, watch, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  getMovieById,
  fetchAvailableScreenings,
  getFirstScreeningDate,
  addFavorite,
  removeFavorite,
  getFavoriteStatus,
  rateMovie,
  getMyRating,
  getMovieRatings,
} from '../services/api'
import type { Movie } from '../types/movie'
import type { Screening } from '../types/screening'
import type { Rating } from '../types/rating'
import type { AxiosError } from 'axios'
import { useAuthStore } from '../stores/auth'
import { ElMessage, ElRate } from 'element-plus'
import { Star, StarFilled } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const movie = ref<Movie | null>(null)
const screenings = ref<Screening[]>([])
const isLoadingMovie = ref(true)
const isLoadingScreenings = ref(true)
const errorMovie = ref<string | null>(null)
const errorScreenings = ref<string | null>(null)
const selectedDate = ref('')

const movieId = ref(route.params.id as string)

const isFavorited = ref(false)
const isLoadingFavoriteStatus = ref(false)
const isTogglingFavorite = ref(false)

const myRating = ref<Rating | null>(null)
const isLoadingMyRating = ref(false)

const comments = ref<Rating[]>([])
const commentPagination = ref({ current: 1, size: 5, total: 0 })
const isLoadingComments = ref(true)

const ratingDialogVisible = ref(false)
const userScore = ref(0)
const userComment = ref('')
const isSubmittingRating = ref(false)

const isLoggedIn = computed(() => authStore.isLoggedIn)

const formatDate = (dateString: string | undefined) => {
  if (!dateString) return 'N/A'
  try {
    return new Date(dateString).toLocaleDateString('zh-CN', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
    })
  } catch (e) {
    console.error('Error formatting date:', e)
    return dateString
  }
}

const formatTime = (dateTimeString: string) => {
  if (!dateTimeString) return 'N/A'
  try {
    return new Date(dateTimeString).toLocaleTimeString('zh-CN', {
      hour: '2-digit',
      minute: '2-digit',
      hour12: false,
    })
  } catch (e) {
    console.error('Error formatting time:', e)
    return dateTimeString // Fallback
  }
}

const getImageUrl = (path: string | undefined) => {
  if (!path) {
    return 'https://via.placeholder.com/300x450.png?text=No+Image' // Larger placeholder
  }
  return path
}

async function loadMovieData(id: string) {
  isLoadingMovie.value = true
  errorMovie.value = null
  movie.value = null
  try {
    const response = await getMovieById(id)
    movie.value = response.data

    // 电影加载成功后，获取收藏状态和用户评分（如果已登录）
    if (isLoggedIn.value) {
      loadFavoriteStatus(id)
      loadMyRating(id)
    }
    loadComments(id, 1) // 加载第一页评论
  } catch (err: unknown) {
    console.error('Failed to load movie details:', err)
    if (typeof err === 'object' && err !== null && 'response' in err) {
      const axiosError = err as AxiosError
      if (axiosError.response && axiosError.response.status === 404) {
        errorMovie.value = '找不到指定的电影。'
      } else {
        errorMovie.value = '无法加载电影详情，请稍后重试。'
      }
    } else {
      errorMovie.value = '无法加载电影详情，发生未知错误。'
    }
  } finally {
    isLoadingMovie.value = false
  }
}

async function loadScreeningsForDate(id: string, date: string) {
  if (!date) {
    screenings.value = []
    errorScreenings.value = null
    isLoadingScreenings.value = false
    return
  }
  isLoadingScreenings.value = true
  errorScreenings.value = null
  screenings.value = []
  try {
    const response = await fetchAvailableScreenings({
      movieId: id,
      date: date,
      size: 100,
    })
    screenings.value = (response.data.records || []).sort(
      (a, b) => new Date(a.startTime).getTime() - new Date(b.startTime).getTime(),
    )
  } catch (err) {
    console.error(`Failed to load screenings for date ${date}:`, err)
    errorScreenings.value = '无法加载场次信息，请稍后重试。'
  } finally {
    isLoadingScreenings.value = false
  }
}

function goToSeatSelection(screeningId: number) {
  router.push(`/book/${screeningId}`)
  console.log(`Navigating to seat selection for screening ID: ${screeningId}`)
}

onMounted(async () => {
  isLoadingMovie.value = true
  errorMovie.value = null
  try {
    await loadMovieData(movieId.value)

    if (movie.value) {
      try {
        const dateResponse = await getFirstScreeningDate(movieId.value)
        const earliestDate = dateResponse.data
        selectedDate.value = earliestDate || new Date().toISOString().split('T')[0]
        await loadScreeningsForDate(movieId.value, selectedDate.value)
      } catch (screeningsOrDateError) {
        console.error(
          'Failed to load first screening date or initial screenings:',
          screeningsOrDateError,
        )
        if (!errorScreenings.value) {
          errorScreenings.value = '加载场次信息时出错。'
        }
        isLoadingScreenings.value = false
        if (!selectedDate.value) {
          selectedDate.value = new Date().toISOString().split('T')[0]
        }
      }
    } else if (!errorMovie.value) {
      errorMovie.value = '无法加载电影数据。'
    }
  } catch (err) {
    console.error('Error in onMounted loading sequence:', err)
    if (!errorMovie.value) {
      errorMovie.value = '加载页面数据时发生错误。'
    }
  } finally {
    isLoadingMovie.value = false
  }
})

watch(
  () => route.params.id,
  (newId) => {
    if (newId && newId !== movieId.value) {
      movieId.value = newId as string
      movie.value = null
      screenings.value = []
      selectedDate.value = ''
      errorMovie.value = null
      errorScreenings.value = null
      isLoadingScreenings.value = true
      ;(async () => {
        isLoadingMovie.value = true
        try {
          await loadMovieData(movieId.value)
          if (movie.value) {
            try {
              const dateResponse = await getFirstScreeningDate(movieId.value)
              const earliestDate = dateResponse.data
              selectedDate.value = earliestDate || new Date().toISOString().split('T')[0]
              await loadScreeningsForDate(movieId.value, selectedDate.value)
            } catch (screeningsOrDateError) {
              console.error(
                'Failed reloading screenings/date on route change:',
                screeningsOrDateError,
              )
              if (!errorScreenings.value) errorScreenings.value = '加载场次信息时出错。'
              isLoadingScreenings.value = false
              if (!selectedDate.value) selectedDate.value = new Date().toISOString().split('T')[0]
            }
          } else if (!errorMovie.value) {
            errorMovie.value = '无法加载电影数据。'
          }
        } catch (err) {
          console.error('Error reloading on route change:', err)
          if (!errorMovie.value) errorMovie.value = '加载页面数据时发生错误。'
        } finally {
          isLoadingMovie.value = false
        }
      })()
    }
  },
)

watch(selectedDate, (newDate, oldDate) => {
  if (newDate && newDate !== oldDate) {
    loadScreeningsForDate(movieId.value, newDate)
  }
})

// 加载收藏状态
async function loadFavoriteStatus(id: string) {
  isLoadingFavoriteStatus.value = true
  try {
    const response = await getFavoriteStatus(id)
    isFavorited.value = response.data.isFavorited
  } catch (error) {
    console.error('无法加载收藏状态:', error)
    // 不提示用户错误，按钮保持默认状态即可
  } finally {
    isLoadingFavoriteStatus.value = false
  }
}

// 加载我的评分
async function loadMyRating(id: string) {
  isLoadingMyRating.value = true
  try {
    const response = await getMyRating(id)
    myRating.value = response.data
    // 如果获取到评分，更新对话框的默认值
    if (myRating.value) {
      userScore.value = myRating.value.score
      userComment.value = myRating.value.comment || ''
    } else {
      userScore.value = 0
      userComment.value = ''
    }
  } catch (error: unknown) {
    if (
      error &&
      typeof error === 'object' &&
      'response' in error &&
      error.response &&
      typeof error.response === 'object' &&
      'status' in error.response &&
      error.response.status === 404
    ) {
      // 404 表示用户未评分，是正常情况
      myRating.value = null
      userScore.value = 0
      userComment.value = ''
    } else {
      console.error('无法加载我的评分:', error)
      // 可以选择性提示错误
    }
  } finally {
    isLoadingMyRating.value = false
  }
}

// 加载评论列表
async function loadComments(id: string, page: number) {
  isLoadingComments.value = true
  try {
    const response = await getMovieRatings(id, page, commentPagination.value.size)
    comments.value = response.data.records || []
    commentPagination.value.total = response.data.total || 0
    commentPagination.value.current = page
  } catch (error) {
    console.error('无法加载评论列表:', error)
    ElMessage.error('加载评论列表失败')
  } finally {
    isLoadingComments.value = false
  }
}

// 切换收藏状态
async function toggleFavorite() {
  if (!isLoggedIn.value) {
    ElMessage.warning('请先登录再收藏电影')
    // 可以选择跳转到登录页: router.push('/login');
    return
  }
  if (isTogglingFavorite.value) return // 防止重复点击

  isTogglingFavorite.value = true
  try {
    if (isFavorited.value) {
      await removeFavorite(movieId.value)
      isFavorited.value = false
      ElMessage.success('已取消收藏')
    } else {
      await addFavorite(movieId.value)
      isFavorited.value = true
      ElMessage.success('已收藏')
    }
  } catch (error) {
    console.error('切换收藏状态失败:', error)
    ElMessage.error('操作失败，请稍后重试')
  } finally {
    isTogglingFavorite.value = false
  }
}

// 打开评分对话框
function openRatingDialog() {
  if (!isLoggedIn.value) {
    ElMessage.warning('请先登录再进行评分')
    return
  }
  // 打开对话框前，确保 score 和 comment 是最新的用户评分
  if (myRating.value) {
    userScore.value = myRating.value.score
    userComment.value = myRating.value.comment || ''
  } else {
    userScore.value = 0
    userComment.value = ''
  }
  ratingDialogVisible.value = true
}

// 提交评分
async function submitRating() {
  if (!isLoggedIn.value) return // 理论上不会发生，因为按钮已限制
  if (userScore.value === 0) {
    ElMessage.warning('请选择评分')
    return
  }

  isSubmittingRating.value = true
  try {
    await rateMovie(movieId.value, {
      score: userScore.value,
      comment: userComment.value,
    })
    ElMessage.success('评分成功')
    ratingDialogVisible.value = false
    // 提交成功后，刷新电影数据（获取新的平均分）和用户评分
    loadMovieData(movieId.value) // 重新加载电影会触发 loadMyRating
    loadComments(movieId.value, 1) // 刷新评论列表到第一页
  } catch (error) {
    console.error('提交评分失败:', error)
    ElMessage.error('提交评分失败，请稍后重试')
  } finally {
    isSubmittingRating.value = false
  }
}

// 评论分页处理
function handleCommentPageChange(newPage: number) {
  loadComments(movieId.value, newPage)
}
</script>

<template>
  <div class="movie-detail-view">
    <div v-if="isLoadingMovie" class="loading-message">正在加载电影信息...</div>

    <div v-else-if="errorMovie" class="error-message">
      {{ errorMovie }}
    </div>

    <div v-else-if="movie" class="movie-content">
      <div class="movie-header">
        <img :src="getImageUrl(movie.posterUrl)" :alt="movie.title" class="movie-poster-detail" />
        <div class="movie-info-main">
          <h1 class="movie-title-detail">{{ movie.title }}</h1>
          <div class="movie-meta-detail">
            <p><strong>导演:</strong> {{ movie.director || '未知' }}</p>
            <p><strong>主演:</strong> {{ movie.actors || '未知' }}</p>
            <p>
              <strong>类型:</strong>
              <span v-if="movie.movieTypes && movie.movieTypes.length > 0">
                {{ movie.movieTypes.map((t) => t.name).join(' / ') }}
              </span>
              <span v-else>未知</span>
            </p>
            <p><strong>国家/地区:</strong> {{ movie.country || '未知' }}</p>
            <p><strong>片长:</strong> {{ movie.duration ? movie.duration + ' 分钟' : '未知' }}</p>
            <p><strong>上映日期:</strong> {{ formatDate(movie.releaseDate) }}</p>
            <p v-if="movie.tmdbVoteAverage">
              <strong>TMDB评分:</strong> {{ movie.tmdbVoteAverage }} / 10 ({{
                movie.tmdbVoteCount || 0
              }}
              票)
            </p>
            <p v-if="movie.averageRating != null">
              <strong>本站评分:</strong>
              <el-rate
                :model-value="movie.averageRating / 2"
                disabled
                show-score
                text-color="#ff9900"
                :score-template="`${movie.averageRating.toFixed(1)} / 10`"
                size="large"
                style="display: inline-flex; vertical-align: middle; margin-left: 5px"
              />
            </p>
          </div>

          <!-- 新增操作按钮区域 -->
          <div class="action-buttons" v-if="isLoggedIn">
            <el-button
              :type="isFavorited ? 'warning' : 'primary'"
              plain
              :icon="isFavorited ? StarFilled : Star"
              @click="toggleFavorite"
              :loading="isTogglingFavorite || isLoadingFavoriteStatus"
              round
            >
              {{ isFavorited ? '已想看' : '想看' }}
            </el-button>
            <el-button
              type="primary"
              plain
              @click="openRatingDialog"
              :loading="isLoadingMyRating"
              round
            >
              {{ myRating ? '修改评分' : '评分 / 写短评' }}
            </el-button>
          </div>
          <div class="action-buttons" v-else>
            <el-button type="primary" plain round @click="router.push('/login')"
              >登录后评分/收藏</el-button
            >
          </div>
          <!-- 操作按钮区域结束 -->
        </div>
      </div>

      <div class="movie-description">
        <h2>剧情简介</h2>
        <p>{{ movie.description || '暂无简介' }}</p>
      </div>

      <div v-if="movie.trailerUrl" class="movie-trailer">
        <h2>预告片</h2>
        <video controls width="640" height="360">
          <source :src="movie.trailerUrl" type="video/mp4" />
          您的浏览器不支持 video 标签。
        </video>
      </div>

      <div class="screenings-section">
        <h2>选择场次</h2>
        <div class="date-selector">
          <label for="screening-date">选择日期:</label>
          <input
            type="date"
            id="screening-date"
            v-model="selectedDate"
            :disabled="isLoadingMovie || !movie"
          />
        </div>

        <div v-if="isLoadingScreenings" class="loading-message">正在加载场次信息...</div>

        <div v-else-if="errorScreenings" class="error-message">
          {{ errorScreenings }}
        </div>

        <div v-else>
          <ul v-if="screenings.length > 0" class="screenings-list">
            <li v-for="screening in screenings" :key="screening.id" class="screening-item">
              <div class="screening-info">
                <span class="screening-time"
                  >{{ formatTime(screening.startTime) }} - {{ formatTime(screening.endTime) }}</span
                >
                <span class="screening-cinema">{{
                  screening.cinemaName || `影院 #${screening.cinemaId}`
                }}</span>
                <span class="screening-room">{{
                  screening.roomName || `影厅 #${screening.roomId}`
                }}</span>
              </div>
              <div class="screening-action">
                <span class="screening-price">¥{{ screening.price.toFixed(2) }}</span>
                <button @click="goToSeatSelection(screening.id)" class="btn-buy">选座购票</button>
              </div>
            </li>
          </ul>
          <p v-else class="no-screenings-message">选择的日期暂无场次安排。</p>
        </div>
      </div>

      <!-- 新增评论区 -->
      <el-divider />
      <div class="comments-section">
        <h2>观众短评</h2>
        <el-skeleton :loading="isLoadingComments" animated :rows="5">
          <template #template>
            <div
              v-for="i in 3"
              :key="i"
              style="
                display: flex;
                align-items: center;
                padding: 10px 0;
                border-bottom: 1px solid #eee;
              "
            >
              <el-skeleton-item
                variant="circle"
                style="margin-right: 15px; width: 40px; height: 40px"
              />
              <div style="flex: 1">
                <el-skeleton-item variant="p" style="width: 30%; margin-bottom: 5px" />
                <el-skeleton-item variant="text" style="width: 20%; margin-bottom: 8px" />
                <el-skeleton-item variant="text" style="width: 80%" />
              </div>
            </div>
          </template>
          <template #default>
            <div v-if="comments.length > 0">
              <div v-for="comment in comments" :key="comment.id" class="comment-item">
                <el-avatar
                  :src="comment.user?.avatar || '/default-avatar.png'"
                  size="small"
                  style="margin-right: 10px"
                ></el-avatar>
                <div class="comment-content">
                  <div class="comment-header">
                    <span class="comment-user">{{
                      comment.user?.nickname || comment.user?.username || '匿名用户'
                    }}</span>
                    <el-rate
                      :model-value="comment.score / 2"
                      disabled
                      size="small"
                      style="margin-left: 10px"
                    />
                  </div>
                  <p class="comment-text">{{ comment.comment || '用户未留下评论' }}</p>
                  <span class="comment-time">{{ formatDate(comment.updateTime) }}</span>
                </div>
              </div>
              <el-pagination
                background
                layout="prev, pager, next"
                :total="commentPagination.total"
                :page-size="commentPagination.size"
                :current-page="commentPagination.current"
                @current-change="handleCommentPageChange"
                :hide-on-single-page="commentPagination.total <= commentPagination.size"
                style="margin-top: 20px; justify-content: center"
              />
            </div>
            <el-empty v-else description="还没有人写短评"></el-empty>
          </template>
        </el-skeleton>
      </div>
      <!-- 评论区结束 -->
    </div>

    <!-- 新增评分对话框 -->
    <el-dialog
      v-model="ratingDialogVisible"
      :title="myRating ? '修改我的评分' : '写短评'"
      width="500px"
      :close-on-click-modal="false"
    >
      <div class="rating-dialog-content">
        <div style="margin-bottom: 10px; text-align: center">
          <el-rate v-model="userScore" :max="10" show-score size="large" />
          <span v-if="userScore > 0" style="margin-left: 10px; color: #ff9900; font-weight: bold">
            {{ userScore }} 分
          </span>
        </div>
        <el-input
          v-model="userComment"
          type="textarea"
          :rows="4"
          placeholder="写下你的评价..."
          maxlength="200"
          show-word-limit
        />
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="ratingDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitRating" :loading="isSubmittingRating">
            提交
          </el-button>
        </span>
      </template>
    </el-dialog>
    <!-- 对话框结束 -->
  </div>
</template>

<style scoped>
.movie-detail-view {
  padding: 2rem;
  max-width: 1000px;
  margin: 2rem auto;
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}

.loading-message,
.error-message,
.no-screenings-message {
  text-align: center;
  padding: 2rem;
  font-size: 1.1rem;
  color: #555;
}

.error-message {
  color: #c62828;
  background-color: #ffebee;
  border-radius: 4px;
}

.movie-content {
  /* Styles for when movie data is loaded */
}

.movie-header {
  display: flex;
  gap: 2rem;
  margin-bottom: 2rem;
  padding-bottom: 2rem;
  border-bottom: 1px solid #eee;
}

.movie-poster-detail {
  width: 250px; /* Adjust size as needed */
  height: auto;
  border-radius: 8px;
  object-fit: cover;
  flex-shrink: 0; /* Prevent image from shrinking */
}

.movie-info-main {
  flex-grow: 1;
}

.movie-title-detail {
  font-size: 2.2rem;
  margin-bottom: 1rem;
  font-weight: 700;
  color: #333;
}

.movie-meta-detail p {
  margin-bottom: 0.6rem;
  font-size: 1rem;
  color: #555;
}

.movie-meta-detail strong {
  color: #333;
  min-width: 80px; /* Align keys */
  display: inline-block;
}

.movie-description,
.movie-trailer,
.screenings-section {
  margin-bottom: 2.5rem;
}

.movie-description h2,
.movie-trailer h2,
.screenings-section h2 {
  font-size: 1.6rem;
  margin-bottom: 1rem;
  padding-bottom: 0.5rem;
  border-bottom: 2px solid #e50914;
  color: #333;
  font-weight: 600;
}

.movie-description p {
  line-height: 1.7;
  color: #444;
}

.movie-trailer video {
  max-width: 100%;
  border-radius: 4px;
}

.date-selector {
  margin-bottom: 1.5rem;
}

.date-selector label {
  margin-right: 0.5rem;
  font-weight: 500;
}

.date-selector input[type='date'] {
  padding: 0.5rem;
  border: 1px solid #ccc;
  border-radius: 4px;
}

.screenings-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.screening-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem 0;
  border-bottom: 1px solid #eee;
}

.screening-item:last-child {
  border-bottom: none;
}

.screening-info {
  display: flex;
  flex-direction: column; /* Stack info on smaller screens if needed */
  gap: 0.3rem;
}

.screening-time {
  font-weight: 600;
  font-size: 1.1rem;
  color: #333;
}

.screening-cinema,
.screening-room {
  color: #666;
  font-size: 0.95rem;
}

.screening-action {
  display: flex;
  align-items: center;
  gap: 1.5rem;
}

.screening-price {
  font-size: 1.2rem;
  font-weight: bold;
  color: #e50914;
}

.btn-buy {
  padding: 0.6rem 1.2rem;
  background-color: #e50914;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-weight: 600;
  transition: background-color 0.3s;
}

.btn-buy:hover {
  background-color: #f40612;
}

@media (max-width: 768px) {
  .movie-header {
    flex-direction: column;
    align-items: center;
    text-align: center;
  }

  .movie-poster-detail {
    width: 200px;
    margin-bottom: 1rem;
  }

  .movie-title-detail {
    font-size: 1.8rem;
  }

  .movie-meta-detail strong {
    min-width: auto; /* Remove alignment */
    display: block; /* Stack key/value */
    margin-bottom: 0.2rem;
  }

  .screening-item {
    flex-direction: column;
    align-items: flex-start;
    gap: 0.8rem;
  }

  .screening-action {
    width: 100%;
    justify-content: space-between;
  }
}

@media (max-width: 480px) {
  .movie-detail-view {
    padding: 1rem;
  }
  .movie-poster-detail {
    width: 150px;
  }
  .movie-title-detail {
    font-size: 1.6rem;
  }
  .movie-meta-detail p {
    font-size: 0.9rem;
  }
  .movie-description h2,
  .movie-trailer h2,
  .screenings-section h2 {
    font-size: 1.4rem;
  }
  .screening-price {
    font-size: 1.1rem;
  }
  .btn-buy {
    padding: 0.5rem 1rem;
  }
}

.action-buttons {
  margin-top: 1.5rem;
  display: flex;
  gap: 10px;
}

.comments-section h2 {
  font-size: 1.6rem;
  margin-bottom: 1.5rem;
  padding-bottom: 0.5rem;
  border-bottom: 2px solid #e50914;
  color: #333;
  font-weight: 600;
}

.comment-item {
  display: flex;
  padding: 15px 0;
  border-bottom: 1px solid #eee;
}

.comment-item:last-child {
  border-bottom: none;
}

.comment-content {
  flex: 1;
}

.comment-header {
  display: flex;
  align-items: center;
  margin-bottom: 5px;
}

.comment-user {
  font-weight: 500;
  font-size: 0.95rem;
  color: #333;
}

.comment-text {
  font-size: 0.9rem;
  line-height: 1.6;
  color: #555;
  margin: 8px 0;
  white-space: pre-wrap; /* 保留换行 */
}

.comment-time {
  font-size: 0.8rem;
  color: #999;
}

.rating-dialog-content {
  padding: 10px 20px;
}

.movie-meta-detail p {
  display: flex; /* 改为 flex 使得 el-rate 能垂直居中 */
  align-items: center;
  min-height: 24px; /* 确保有评分和没评分时高度一致 */
}

.movie-meta-detail strong {
  margin-right: 5px; /* 给 strong 加点右边距 */
}
</style>
