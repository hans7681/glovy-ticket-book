<script setup lang="ts">
import { ref, onMounted, watch, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  getMovieById,
  fetchAvailableScreenings,
  getFirstScreeningDate,
  getFavoriteStatus,
  addFavorite,
  removeFavorite,
  getMyRating,
  getMovieRatings,
  rateMovie,
} from '@/services/api'
import { useAuthStore } from '@/stores/auth'
import type { Movie } from '@/types/movie'
import type { Screening } from '@/types/screening'
import type { Rating } from '@/types/rating'
import {
  ElMessage,
  ElRate,
  ElButton,
  ElDivider,
  ElDatePicker,
  ElAlert,
  ElEmpty,
  ElSkeleton,
} from 'element-plus'
import { StarFilled } from '@element-plus/icons-vue'
import { getFutureDateOptions, formatDateToYYYYMMDD } from '@/utils/datetime'
import type { AxiosError } from 'axios'

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

const dateOptions = ref<{ label: string; value: string }[]>([])

const initializeDateOptions = () => {
  dateOptions.value = getFutureDateOptions(7)
}

const setDefaultDate = async () => {
  try {
    const response = await getFirstScreeningDate(movieId.value)
    if (response.data) {
      selectedDate.value = response.data
    } else {
      selectedDate.value = formatDateToYYYYMMDD(new Date())
    }
  } catch (err) {
    console.warn('无法获取最早场次日期，默认设置为今天:', err)
    selectedDate.value = formatDateToYYYYMMDD(new Date())
  }
}

const disabledDate = (time: Date) => {
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  return time.getTime() < today.getTime()
}

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
    return dateTimeString
  }
}

const getImageUrl = (path: string | undefined) => {
  if (!path) {
    return 'https://via.placeholder.com/300x450.png?text=No+Image'
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

    if (isLoggedIn.value) {
      loadFavoriteStatus(id)
      loadMyRating(id)
    }
    loadComments(id, 1)
  } catch (err: unknown) {
    console.error('获取电影详情失败:', err)
    if (err && typeof err === 'object' && 'response' in err) {
      const axiosError = err as AxiosError<{ message?: string }>
      if (axiosError.response?.status === 404) {
        errorMovie.value = '找不到指定的电影。'
      } else {
        errorMovie.value = '加载电影详情失败。'
      }
    } else {
      errorMovie.value = '加载电影详情失败。'
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
  } catch (err: unknown) {
    console.error('获取场次失败:', err)
    let message = '加载场次信息失败，请稍后再试。'
    if (err && typeof err === 'object' && 'response' in err) {
      const axiosError = err as AxiosError<{ message?: string }>
      if (axiosError.response?.data?.message) {
        message = `加载场次失败: ${axiosError.response.data.message}`
      } else if (axiosError.response?.status) {
        message = `加载场次失败 (HTTP ${axiosError.response.status})。`
      }
    }
    errorScreenings.value = message
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
    initializeDateOptions()
    await setDefaultDate()

    if (movie.value) {
      try {
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

async function loadFavoriteStatus(id: string) {
  isLoadingFavoriteStatus.value = true
  try {
    const response = await getFavoriteStatus(id)
    isFavorited.value = response.data.isFavorited
  } catch (error) {
    console.error('无法加载收藏状态:', error)
  } finally {
    isLoadingFavoriteStatus.value = false
  }
}

async function loadMyRating(id: string) {
  isLoadingMyRating.value = true
  try {
    const response = await getMyRating(id)
    myRating.value = response.data
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
      myRating.value = null
      userScore.value = 0
      userComment.value = ''
    } else {
      console.error('无法加载我的评分:', error)
    }
  } finally {
    isLoadingMyRating.value = false
  }
}

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

async function toggleFavorite() {
  if (!isLoggedIn.value) {
    ElMessage.warning('请先登录再收藏电影')
    return
  }
  if (isTogglingFavorite.value) return

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

function openRatingDialog() {
  if (!isLoggedIn.value) {
    ElMessage.warning('请先登录再进行评分')
    return
  }
  if (myRating.value) {
    userScore.value = myRating.value.score
    userComment.value = myRating.value.comment || ''
  } else {
    userScore.value = 0
    userComment.value = ''
  }
  ratingDialogVisible.value = true
}

async function submitRating() {
  if (!isLoggedIn.value) return
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
    loadMovieData(movieId.value)
    loadComments(movieId.value, 1)
  } catch (error) {
    console.error('提交评分失败:', error)
    ElMessage.error('提交评分失败，请稍后重试')
  } finally {
    isSubmittingRating.value = false
  }
}

function handleCommentPageChange(newPage: number) {
  loadComments(movieId.value, newPage)
}

const handleDateChange = (newDate: string | null) => {
  console.log('Date picker changed to:', newDate)
}

const selectDateFromButton = (dateValue: string) => {
  selectedDate.value = dateValue
  console.log('Date button clicked:', dateValue)
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

          <div class="action-buttons" v-if="isLoggedIn">
            <el-button
              :type="isFavorited ? 'warning' : 'primary'"
              plain
              :icon="isFavorited ? StarFilled : StarFilled"
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

      <div class="screenings-section" v-loading="isLoadingScreenings">
        <h3 class="section-title">选择场次</h3>

        <div class="date-filter-container">
          <span class="date-label">选择日期：</span>
          <el-date-picker
            v-model="selectedDate"
            type="date"
            placeholder="选择日期"
            format="YYYY/MM/DD"
            value-format="YYYY-MM-DD"
            :disabled-date="disabledDate"
            @change="handleDateChange"
            style="margin-right: 10px"
          />
          <div class="date-quick-select-bar">
            <el-button
              v-for="dateOption in dateOptions"
              :key="dateOption.value"
              :type="selectedDate === dateOption.value ? 'primary' : ''"
              text
              size="small"
              class="date-button"
              @click="selectDateFromButton(dateOption.value)"
            >
              {{ dateOption.label }}
            </el-button>
          </div>
        </div>

        <el-alert
          v-if="errorScreenings"
          :title="errorScreenings"
          type="error"
          show-icon
          :closable="false"
          style="margin-bottom: 15px"
        />

        <template v-if="screenings.length > 0">
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
        </template>
        <el-empty
          v-else-if="!isLoadingScreenings && !errorScreenings && selectedDate"
          description="所选日期暂无场次安排"
        />
        <el-empty v-else-if="!isLoadingScreenings && !selectedDate" description="请先选择日期" />
      </div>

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
    </div>

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

.screenings-section {
  margin-top: 30px;
  background-color: #fff;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.section-title {
  font-size: 1.4rem;
  font-weight: 600;
  margin-bottom: 20px;
  color: #333;
  padding-bottom: 10px;
  border-bottom: 2px solid #e50914; /* Accent color border */
  display: inline-block;
}

.date-filter-container {
  display: flex;
  align-items: center;
  flex-wrap: nowrap; /* Prevent wrapping initially */
  margin-bottom: 25px;
  padding: 10px;
  background-color: #f8f9fa;
  border-radius: 6px;
  overflow-x: hidden; /* Hide horizontal scrollbar on the main container */
}

.date-label {
  font-weight: 500;
  margin-right: 8px;
  color: #555;
  white-space: nowrap; /* Prevent label from wrapping */
}

.date-quick-select-bar {
  display: flex;
  gap: 5px; /* Smaller gap between buttons */
  overflow-x: auto; /* Enable horizontal scrolling */
  padding-bottom: 5px; /* Add some space for scrollbar if it appears */
  /* Hide scrollbar visually - cross-browser */
  -ms-overflow-style: none; /* IE and Edge */
  scrollbar-width: none; /* Firefox */
}
.date-quick-select-bar::-webkit-scrollbar {
  display: none; /* Chrome, Safari, Opera */
}

.date-button {
  padding: 5px 10px; /* Adjust padding for smaller buttons */
  font-size: 0.85rem;
  white-space: nowrap; /* Prevent button text wrapping */
  border: 1px solid transparent; /* Add transparent border for consistent height */
  transition:
    background-color 0.2s,
    border-color 0.2s,
    color 0.2s;
  margin: 0; /* Remove default margins */
}

.date-button.el-button.is-text:hover,
.date-button.el-button.is-text:focus {
  /* Custom hover for text buttons */
  background-color: #ecf5ff;
  color: #409eff;
  border-color: #c6e2ff;
}

/* Style for the primary (active) text button */
.date-button.el-button--primary.is-text {
  background-color: #409eff; /* Primary background */
  color: #fff; /* White text */
  border-color: #409eff;
}
.date-button.el-button--primary.is-text:hover,
.date-button.el-button--primary.is-text:focus {
  background-color: #66b1ff; /* Lighter blue on hover/focus */
  border-color: #66b1ff;
  color: #fff;
}

/* Responsive adjustments if needed */
@media (max-width: 768px) {
  .date-filter-container {
    /* Allow wrapping on smaller screens if needed */
    /* flex-wrap: wrap; */
    padding: 8px;
  }
  .date-quick-select-bar {
    margin-top: 10px; /* Add space if date picker wraps */
    width: 100%; /* Ensure it takes full width when wrapped */
  }
}

/* Styles for cinema groups and screenings */
.cinema-group {
  margin-bottom: 25px;
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  overflow: hidden; /* Ensures border-radius applies correctly */
}

.cinema-header {
  background-color: #f7f7f7;
  padding: 12px 15px;
  font-weight: bold;
  color: #333;
  border-bottom: 1px solid #e0e0e0;
}

.screening-list {
  padding: 0;
  margin: 0;
  list-style: none;
}

.screening-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px;
  border-bottom: 1px dashed #eee; /* Dashed separator */
}
.screening-item:last-child {
  border-bottom: none;
}

.screening-time {
  font-size: 1.1rem;
  font-weight: 600;
  color: #333;
  width: 150px; /* Fixed width for alignment */
}

.screening-room {
  color: #666;
  font-size: 0.9rem;
  width: 150px; /* Fixed width for alignment */
  text-align: center;
}

.screening-price {
  font-size: 1.2rem;
  color: #e50914; /* Accent color for price */
  font-weight: bold;
  width: 100px; /* Fixed width */
  text-align: right;
}

.select-seat-button {
  margin-left: 20px;
  width: 100px; /* Fixed width */
}
</style>
