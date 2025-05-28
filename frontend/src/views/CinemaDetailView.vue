<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getCinemaById, fetchAvailableScreenings, getMovieById } from '../services/api' // Import getMovieById
import type { Cinema } from '../types/cinema'
import type { Screening } from '../types/screening'
import type { Movie } from '../types/movie' // Import Movie type
import type { AxiosError } from 'axios'
// Import date utils and Element Plus components
import { ElDatePicker, ElButton, ElAlert, ElEmpty } from 'element-plus'
import { getFutureDateOptions, formatDateToYYYYMMDD } from '@/utils/datetime'

interface MovieBasicInfo {
  id: number
  title: string
  posterUrl?: string // Make posterUrl optional here as well
}

interface MovieWithScreenings {
  movie: Movie | MovieBasicInfo // Use the basic info interface
  screenings: Screening[]
}

const route = useRoute()
const router = useRouter()

const cinema = ref<Cinema | null>(null)
const screeningsGroupedByMovie = ref<MovieWithScreenings[]>([])
const isLoadingCinema = ref(true)
const isLoadingScreenings = ref(true)
const errorCinema = ref<string | null>(null)
const errorScreenings = ref<string | null>(null)
// Use formatDateToYYYYMMDD for default date
const selectedDate = ref(formatDateToYYYYMMDD(new Date()))

const cinemaId = ref(route.params.id as string)

// Cache for movie details to avoid fetching the same movie multiple times
const movieCache = ref<Record<number, Movie>>({})

// Add ref for date options
const dateOptions = ref<{ label: string; value: string }[]>([])

// Initialize date options
const initializeDateOptions = () => {
  dateOptions.value = getFutureDateOptions(6) // 获取今天及未来5天 (共6个)
}

// Disable dates before today
const disabledDate = (time: Date) => {
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  return time.getTime() < today.getTime()
}

// Helper to format time (same as MovieDetailView)
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

// Function to construct full image URL for logo (same as CinemaListView)
const getLogoUrl = (path: string | undefined) => {
  if (!path) {
    return 'https://via.placeholder.com/150x75.png?text=No+Logo'
  }
  return path
}

// Function to construct full image URL for movie posters
const getMoviePosterUrl = (path: string | undefined) => {
  if (!path) {
    return 'https://via.placeholder.com/100x150.png?text=No+Image'
  }
  return path
}

async function loadCinemaDetail(id: string) {
  isLoadingCinema.value = true
  errorCinema.value = null
  cinema.value = null
  try {
    const response = await getCinemaById(id)
    cinema.value = response.data
  } catch (err: unknown) {
    console.error('Failed to load cinema details:', err)
    if (typeof err === 'object' && err !== null && 'response' in err) {
      const axiosError = err as AxiosError
      if (axiosError.response && axiosError.response.status === 404) {
        errorCinema.value = '找不到指定的影院或该影院未批准。'
      } else {
        errorCinema.value = '无法加载影院详情，请稍后重试。'
      }
    } else {
      errorCinema.value = '无法加载影院详情，发生未知错误。'
    }
  } finally {
    isLoadingCinema.value = false
  }
}

async function loadScreeningsForCinema(id: string, date: string) {
  if (!date) {
    screeningsGroupedByMovie.value = []
    errorScreenings.value = null
    isLoadingScreenings.value = false
    return
  }
  isLoadingScreenings.value = true
  errorScreenings.value = null
  screeningsGroupedByMovie.value = []
  try {
    const response = await fetchAvailableScreenings({
      cinemaId: id,
      date: date,
      size: 500, // Fetch a large number, assuming one cinema won't have >500 screenings/day
    })

    const screeningsRaw = response.data.records

    if (!screeningsRaw || screeningsRaw.length === 0) {
      isLoadingScreenings.value = false
      console.log(`日期 ${date} 没有查询到场次信息。`)
      return // No screenings for this date
    }

    // Group screenings by movieId
    const grouped: Record<number, Screening[]> = {}
    const movieIds = new Set<number>()
    screeningsRaw.forEach((s) => {
      if (!grouped[s.movieId]) {
        grouped[s.movieId] = []
      }
      grouped[s.movieId].push(s)
      movieIds.add(s.movieId)
    })

    // Fetch details for movies not in cache
    const moviesToFetch = Array.from(movieIds).filter((mid) => !movieCache.value[mid])
    if (moviesToFetch.length > 0) {
      for (const mid of moviesToFetch) {
        try {
          const movieResponse = await getMovieById(mid)
          movieCache.value[mid] = movieResponse.data
        } catch (movieErr) {
          console.warn(`Failed to fetch details for movie ${mid}:`, movieErr)
          movieCache.value[mid] = { id: mid, title: `电影 #${mid}` } as MovieBasicInfo
        }
      }
    }

    // Build the final structure
    const result: MovieWithScreenings[] = []
    for (const movieIdStr in grouped) {
      const movieIdNum = parseInt(movieIdStr, 10)
      // Ensure movieInfo is treated potentially as MovieBasicInfo if full details failed
      const movieInfo: Movie | MovieBasicInfo = movieCache.value[movieIdNum] || {
        id: movieIdNum,
        title: `电影 #${movieIdNum}`,
      }
      result.push({
        movie: movieInfo,
        screenings: grouped[movieIdNum].sort(
          (a, b) => new Date(a.startTime).getTime() - new Date(b.startTime).getTime(),
        ),
      })
    }

    // Sort movies by title
    result.sort((a, b) => {
      const titleA = typeof a.movie.title === 'string' ? a.movie.title : ''
      const titleB = typeof b.movie.title === 'string' ? b.movie.title : ''
      return titleA.localeCompare(titleB, 'zh-CN') // Use localeCompare for better sorting
    })

    screeningsGroupedByMovie.value = result
  } catch (err) {
    console.error('Failed to load screenings:', err)
    errorScreenings.value = '无法加载场次信息，请稍后重试。'
  } finally {
    isLoadingScreenings.value = false
  }
}

// Navigate to seat selection (placeholder - same as MovieDetailView)
function goToSeatSelection(screeningId: number) {
  router.push(`/book/${screeningId}`)
  console.log(`Navigating to seat selection for screening ID: ${screeningId}`)
}

// Date picker change handler
const handleDateChange = (newDate: string | null) => {
  // v-model already updated selectedDate
  console.log('Date picker changed to:', newDate)
  // Watcher will trigger fetch
}

// Date button click handler
const selectDateFromButton = (dateValue: string) => {
  selectedDate.value = dateValue
  console.log('Date button clicked:', dateValue)
  // Watcher will trigger fetch
}

// Load initial data
onMounted(() => {
  initializeDateOptions() // Initialize date buttons first
  loadCinemaDetail(cinemaId.value)
  // Watcher will handle the initial screening load based on default selectedDate
  // loadScreeningsForCinema(cinemaId.value, selectedDate.value); // Removed, watcher handles it
})

// Watch for route param changes
watch(
  () => route.params.id,
  (newId) => {
    if (newId && newId !== cinemaId.value) {
      cinemaId.value = newId as string
      loadCinemaDetail(cinemaId.value)
      // Reset date and let watcher load screenings for the new cinema
      selectedDate.value = formatDateToYYYYMMDD(new Date())
      // loadScreeningsForCinema(cinemaId.value, selectedDate.value); // Watcher handles it
    }
  },
)

// Watch for selected date changes
watch(
  selectedDate,
  (newDate, oldDate) => {
    if (newDate && newDate !== oldDate) {
      console.log(`Selected date changed from ${oldDate} to ${newDate}, fetching screenings...`)
      loadScreeningsForCinema(cinemaId.value, newDate)
    }
  },
  { immediate: true },
) // Load screenings immediately when component mounts or date changes
</script>

<template>
  <div class="cinema-detail-view">
    <!-- Loading Cinema State -->
    <div v-if="isLoadingCinema" class="loading-message">正在加载影院信息...</div>

    <!-- Error Cinema State -->
    <div v-else-if="errorCinema" class="error-message">
      {{ errorCinema }}
    </div>

    <!-- Cinema Content -->
    <div v-else-if="cinema" class="cinema-content">
      <div class="cinema-header">
        <img
          :src="getLogoUrl(cinema.logo)"
          :alt="cinema.name + ' Logo'"
          class="cinema-logo-detail"
        />
        <div class="cinema-info-main">
          <h1 class="cinema-title-detail">{{ cinema.name }}</h1>
          <p><strong>地址:</strong> {{ cinema.address }}</p>
          <p v-if="cinema.phone"><strong>电话:</strong> {{ cinema.phone }}</p>
          <p v-if="cinema.description"><strong>简介:</strong> {{ cinema.description }}</p>
        </div>
      </div>

      <div class="screenings-section">
        <h2 class="section-title">选择场次</h2>
        <!-- Updated Date Filter -->
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

        <!-- Loading Screenings State -->
        <div v-if="isLoadingScreenings" class="loading-message">正在加载场次信息...</div>

        <!-- Error Screenings State -->
        <el-alert
          v-else-if="errorScreenings"
          :title="errorScreenings"
          type="error"
          show-icon
          :closable="false"
          style="margin-bottom: 15px"
        />

        <!-- Screenings Grouped by Movie -->
        <div v-else>
          <div v-if="screeningsGroupedByMovie.length > 0" class="movie-groups">
            <div
              v-for="group in screeningsGroupedByMovie"
              :key="group.movie.id"
              class="movie-group"
            >
              <RouterLink :to="'/movies/' + group.movie.id" class="movie-group-header">
                <img
                  :src="getMoviePosterUrl((group.movie as Movie)?.posterUrl)"
                  :alt="group.movie.title"
                  class="movie-group-poster"
                />
                <h3 class="movie-group-title">{{ group.movie.title }}</h3>
              </RouterLink>
              <ul class="screenings-list">
                <li
                  v-for="screening in group.screenings"
                  :key="screening.id"
                  class="screening-item"
                >
                  <div class="screening-info">
                    <span class="screening-time"
                      >{{ formatTime(screening.startTime) }} -
                      {{ formatTime(screening.endTime) }}</span
                    >
                    <span class="screening-room">{{
                      screening.roomName || `影厅 #${screening.roomId}`
                    }}</span>
                  </div>
                  <div class="screening-action">
                    <span class="screening-price">¥{{ screening.price.toFixed(2) }}</span>
                    <el-button
                      type="danger"
                      size="small"
                      @click="goToSeatSelection(screening.id)"
                      class="btn-buy"
                      >选座购票</el-button
                    >
                  </div>
                </li>
              </ul>
            </div>
          </div>
          <!-- No Screenings Message -->
          <el-empty v-else description="所选日期暂无场次安排" />
        </div>
      </div>
    </div>

    <!-- Fallback if cinema is null after loading -->
    <div v-else class="error-message">无法加载影院信息。</div>
  </div>
</template>

<style scoped>
.cinema-detail-view {
  padding: 2rem;
  max-width: 1000px;
  margin: 2rem auto;
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}

.loading-message,
.error-message {
  text-align: center;
  padding: 2rem;
  font-size: 1.1rem;
  color: #555;
}

.error-message {
  color: #c62828; /* Material Design Red 700 */
  background-color: #ffebee; /* Material Design Red 50 */
  border-radius: 4px;
}

.cinema-content {
  /* Styles for when cinema data is loaded */
}

.cinema-header {
  display: flex;
  gap: 1.5rem;
  margin-bottom: 2rem;
  padding-bottom: 1.5rem;
  border-bottom: 1px solid #eee;
}

.cinema-logo-detail {
  width: 150px;
  height: 75px; /* Maintain aspect ratio if needed */
  object-fit: contain; /* Use contain to avoid cropping logo */
  border-radius: 4px;
  flex-shrink: 0;
  border: 1px solid #eee;
}

.cinema-info-main {
  flex-grow: 1;
}

.cinema-title-detail {
  font-size: 1.8rem;
  margin-bottom: 0.5rem;
  font-weight: 600;
  color: #333;
}

.cinema-info-main p {
  margin-bottom: 0.4rem;
  font-size: 0.95rem;
  color: #555;
  line-height: 1.5;
}

.cinema-info-main strong {
  color: #333;
  margin-right: 5px;
}

.screenings-section {
  margin-top: 30px;
  /* Removed background/padding/shadow from here to match MovieDetailView */
}

.section-title {
  /* Copied from MovieDetailView */
  font-size: 1.4rem;
  font-weight: 600;
  margin-bottom: 20px;
  color: #333;
  padding-bottom: 10px;
  border-bottom: 2px solid #e50914; /* Accent color */
  display: inline-block;
}

/* Copied Date Filter Styles from MovieDetailView */
.date-filter-container {
  display: flex;
  align-items: center;
  flex-wrap: nowrap;
  margin-bottom: 25px;
  padding: 10px;
  background-color: #f8f9fa;
  border-radius: 6px;
  overflow-x: hidden;
}

.date-label {
  font-weight: 500;
  margin-right: 8px;
  color: #555;
  white-space: nowrap;
}

.date-quick-select-bar {
  display: flex;
  gap: 5px;
  overflow-x: auto;
  padding-bottom: 5px;
  -ms-overflow-style: none;
  scrollbar-width: none;
}
.date-quick-select-bar::-webkit-scrollbar {
  display: none;
}

.date-button {
  padding: 5px 10px;
  font-size: 0.85rem;
  white-space: nowrap;
  border: 1px solid transparent;
  transition:
    background-color 0.2s,
    border-color 0.2s,
    color 0.2s;
  margin: 0;
}

.date-button.el-button.is-text:hover,
.date-button.el-button.is-text:focus {
  background-color: #ecf5ff;
  color: #409eff;
  border-color: #c6e2ff;
}

.date-button.el-button--primary.is-text {
  background-color: #409eff;
  color: #fff;
  border-color: #409eff;
}
.date-button.el-button--primary.is-text:hover,
.date-button.el-button--primary.is-text:focus {
  background-color: #66b1ff;
  border-color: #66b1ff;
  color: #fff;
}

/* --- End Copied Styles --- */

.movie-groups {
  /* Add styling for the container of all movie groups if needed */
}

.movie-group {
  margin-bottom: 2rem;
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  background-color: #fff; /* White background for each movie group */
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.05);
  overflow: hidden;
}

.movie-group-header {
  display: flex;
  align-items: center;
  gap: 15px;
  padding: 15px;
  background-color: #f9f9f9; /* Light background for header */
  border-bottom: 1px solid #e0e0e0;
  text-decoration: none; /* Remove underline from link */
  color: inherit; /* Inherit text color */
  transition: background-color 0.2s ease;
}

.movie-group-header:hover {
  background-color: #f0f0f0; /* Slightly darker on hover */
}

.movie-group-poster {
  width: 50px; /* Smaller poster */
  height: 75px;
  object-fit: cover;
  border-radius: 4px;
  flex-shrink: 0;
}

.movie-group-title {
  font-size: 1.2rem;
  font-weight: 600;
  color: #333;
  margin: 0;
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
  padding: 12px 15px;
  border-bottom: 1px dashed #eee;
}
.screening-item:last-child {
  border-bottom: none;
}

.screening-info {
  display: flex;
  align-items: center;
  gap: 15px; /* Space between time and room */
  flex-grow: 1;
}

.screening-time {
  font-weight: 600;
  font-size: 1rem;
  color: #333;
  width: 130px; /* Adjust width */
}

.screening-room {
  color: #666;
  font-size: 0.9rem;
}

.screening-action {
  display: flex;
  align-items: center;
  gap: 1rem;
  width: 180px; /* Fixed width for price + button */
  justify-content: flex-end;
}

.screening-price {
  font-size: 1.1rem;
  font-weight: bold;
  color: #e50914; /* Accent color */
  width: 70px;
  text-align: right;
}

.btn-buy {
  /* Inherit styles or define specific ones */
  /* Example using ElButton classes might be better for consistency */
  /* padding: 0.5rem 1rem; */
  /* font-size: 0.9rem; */
}

@media (max-width: 768px) {
  .date-filter-container {
    /* Allow wrapping on smaller screens if needed */
    flex-wrap: wrap;
    padding: 8px;
  }
  .date-quick-select-bar {
    margin-top: 10px; /* Add space if date picker wraps */
    width: 100%; /* Ensure it takes full width when wrapped */
  }
  .cinema-header {
    flex-direction: column;
    align-items: center;
    text-align: center;
  }
  .screening-item {
    flex-direction: column;
    align-items: flex-start;
    gap: 0.5rem;
  }
  .screening-action {
    width: 100%;
    justify-content: space-between;
    margin-top: 8px;
  }
}
</style>
