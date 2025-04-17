<script setup lang="ts">
import { ref, onMounted, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { getCinemaById, fetchAvailableScreenings, getMovieById } from '../services/api'; // Import getMovieById
import type { Cinema } from '../types/cinema';
import type { Screening } from '../types/screening';
import type { Movie } from '../types/movie'; // Import Movie type
import type { AxiosError } from 'axios';

interface MovieBasicInfo {
  id: number;
  title: string;
  posterUrl?: string; // Make posterUrl optional here as well
}

interface MovieWithScreenings {
  movie: Movie | MovieBasicInfo; // Use the basic info interface
  screenings: Screening[];
}

const route = useRoute();
const router = useRouter();

const cinema = ref<Cinema | null>(null);
const screeningsGroupedByMovie = ref<MovieWithScreenings[]>([]);
const isLoadingCinema = ref(true);
const isLoadingScreenings = ref(true);
const errorCinema = ref<string | null>(null);
const errorScreenings = ref<string | null>(null);
const selectedDate = ref(new Date().toISOString().split('T')[0]); // Default to today

const cinemaId = ref(route.params.id as string);

// Cache for movie details to avoid fetching the same movie multiple times
const movieCache = ref<Record<number, Movie>>({});

// Helper to format time (same as MovieDetailView)
const formatTime = (dateTimeString: string) => {
  if (!dateTimeString) return 'N/A';
  try {
    return new Date(dateTimeString).toLocaleTimeString('zh-CN', {
      hour: '2-digit', minute: '2-digit', hour12: false
    });
  } catch (e) {
    console.error('Error formatting time:', e);
    return dateTimeString;
  }
};

// Function to construct full image URL for logo (same as CinemaListView)
const getLogoUrl = (path: string | undefined) => {
  if (!path) {
    return 'https://via.placeholder.com/150x75.png?text=No+Logo';
  }
  return path;
};

// Function to construct full image URL for movie posters
const getMoviePosterUrl = (path: string | undefined) => {
  if (!path) {
    return 'https://via.placeholder.com/100x150.png?text=No+Image';
  }
  return path;
};

async function loadCinemaDetail(id: string) {
  isLoadingCinema.value = true;
  errorCinema.value = null;
  cinema.value = null;
  try {
    const response = await getCinemaById(id);
    cinema.value = response.data;
  } catch (err: unknown) {
    console.error("Failed to load cinema details:", err);
    if (typeof err === 'object' && err !== null && 'response' in err) {
      const axiosError = err as AxiosError;
      if (axiosError.response && axiosError.response.status === 404) {
        errorCinema.value = "找不到指定的影院或该影院未批准。";
      } else {
        errorCinema.value = "无法加载影院详情，请稍后重试。";
      }
    } else {
      errorCinema.value = "无法加载影院详情，发生未知错误。";
    }
  } finally {
    isLoadingCinema.value = false;
  }
}

async function loadScreeningsForCinema(id: string, date: string) {
  isLoadingScreenings.value = true;
  errorScreenings.value = null;
  screeningsGroupedByMovie.value = [];
  try {
    const response = await fetchAvailableScreenings({
      cinemaId: id,
      date: date,
      size: 500 // Fetch a large number, assuming one cinema won't have >500 screenings/day
    });

    const screeningsRaw = response.data.records;

    if (screeningsRaw.length === 0) {
      isLoadingScreenings.value = false;
      return; // No screenings for this date
    }

    // Group screenings by movieId
    const grouped: Record<number, Screening[]> = {};
    const movieIds = new Set<number>();
    screeningsRaw.forEach(s => {
      if (!grouped[s.movieId]) {
        grouped[s.movieId] = [];
      }
      grouped[s.movieId].push(s);
      movieIds.add(s.movieId);
    });

    // Fetch details for movies not in cache
    const moviesToFetch = Array.from(movieIds).filter(mid => !movieCache.value[mid]);
    if (moviesToFetch.length > 0) {
        // Consider fetching in parallel if many movies
        for (const mid of moviesToFetch) {
             try {
                const movieResponse = await getMovieById(mid);
                movieCache.value[mid] = movieResponse.data;
             } catch (movieErr) {
                 console.warn(`Failed to fetch details for movie ${mid}:`, movieErr);
                 // Use basic info if fetch fails - ensure it matches MovieBasicInfo
                 movieCache.value[mid] = { id: mid, title: `电影 #${mid}` } as MovieBasicInfo; // Cast to MovieBasicInfo
             }
        }
    }

    // Build the final structure
    const result: MovieWithScreenings[] = [];
    for (const movieIdStr in grouped) {
        const movieIdNum = parseInt(movieIdStr, 10);
        const movieInfo = movieCache.value[movieIdNum] || { id: movieIdNum, title: `电影 #${movieIdNum}` } as MovieBasicInfo;
        result.push({
            movie: movieInfo,
            screenings: grouped[movieIdNum].sort((a, b) =>
                new Date(a.startTime).getTime() - new Date(b.startTime).getTime()
            )
        });
    }

    // Sort movies (e.g., by title or release date if available)
    result.sort((a, b) => {
        const titleA = typeof a.movie.title === 'string' ? a.movie.title : '';
        const titleB = typeof b.movie.title === 'string' ? b.movie.title : '';
        return titleA.localeCompare(titleB);
    });

    screeningsGroupedByMovie.value = result;

  } catch (err) {
    console.error("Failed to load screenings:", err);
    errorScreenings.value = "无法加载场次信息，请稍后重试。";
  } finally {
    isLoadingScreenings.value = false;
  }
}

// Navigate to seat selection (placeholder - same as MovieDetailView)
function goToSeatSelection(screeningId: number) {
  router.push(`/book/${screeningId}`);
  console.log(`Navigating to seat selection for screening ID: ${screeningId}`);
}

// Load initial data
onMounted(() => {
  loadCinemaDetail(cinemaId.value);
  loadScreeningsForCinema(cinemaId.value, selectedDate.value);
});

// Watch for route param changes
watch(() => route.params.id, (newId) => {
  if (newId && newId !== cinemaId.value) {
    cinemaId.value = newId as string;
    loadCinemaDetail(cinemaId.value);
    // Reset date and load screenings for the new cinema
    selectedDate.value = new Date().toISOString().split('T')[0];
    loadScreeningsForCinema(cinemaId.value, selectedDate.value);
  }
});

// Watch for selected date changes
watch(selectedDate, (newDate) => {
  loadScreeningsForCinema(cinemaId.value, newDate);
});

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
        <img :src="getLogoUrl(cinema.logo)" :alt="cinema.name + ' Logo'" class="cinema-logo-detail" />
        <div class="cinema-info-main">
          <h1 class="cinema-title-detail">{{ cinema.name }}</h1>
          <p><strong>地址:</strong> {{ cinema.address }}</p>
          <p v-if="cinema.phone"><strong>电话:</strong> {{ cinema.phone }}</p>
          <p v-if="cinema.description"><strong>简介:</strong> {{ cinema.description }}</p>
        </div>
      </div>

      <div class="screenings-section">
        <h2>场次安排</h2>
        <div class="date-selector">
          <label for="screening-date">选择日期:</label>
          <input type="date" id="screening-date" v-model="selectedDate" />
        </div>

        <!-- Loading Screenings State -->
        <div v-if="isLoadingScreenings" class="loading-message">正在加载场次信息...</div>

        <!-- Error Screenings State -->
        <div v-else-if="errorScreenings" class="error-message">
          {{ errorScreenings }}
        </div>

        <!-- Screenings Grouped by Movie -->
        <div v-else>
          <div v-if="screeningsGroupedByMovie.length > 0" class="movie-groups">
            <div v-for="group in screeningsGroupedByMovie" :key="group.movie.id" class="movie-group">
              <RouterLink :to="'/movies/' + group.movie.id" class="movie-group-header">
                 <img :src="getMoviePosterUrl(group.movie?.posterUrl)" :alt="group.movie.title" class="movie-group-poster" />
                 <h3 class="movie-group-title">{{ group.movie.title }}</h3>
                 <!-- Optional: Add movie meta like duration -->
              </RouterLink>
              <ul class="screenings-list">
                <li v-for="screening in group.screenings" :key="screening.id" class="screening-item">
                  <div class="screening-info">
                    <span class="screening-time">{{ formatTime(screening.startTime) }} - {{ formatTime(screening.endTime) }}</span>
                    <span class="screening-room">{{ screening.roomName || `影厅 #${screening.roomId}` }}</span>
                  </div>
                  <div class="screening-action">
                    <span class="screening-price">¥{{ screening.price.toFixed(2) }}</span>
                    <button @click="goToSeatSelection(screening.id)" class="btn-buy">选座购票</button>
                  </div>
                </li>
              </ul>
            </div>
          </div>
          <p v-else class="no-screenings-message">选择的日期该影院暂无场次安排。</p>
        </div>
      </div>
    </div>

  </div>
</template>

<style scoped>
.cinema-detail-view {
  padding: 2rem;
  max-width: 1000px;
  margin: 2rem auto;
  background-color: #f9f9f9; /* Lighter background */
  border-radius: 8px;
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

.cinema-content {
  /* Styles for loaded content */
}

.cinema-header {
  display: flex;
  gap: 2rem;
  padding: 2rem;
  background-color: #fff;
  border-radius: 8px;
  margin-bottom: 2rem;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  align-items: flex-start; /* Align items top */
}

.cinema-logo-detail {
  width: 150px; /* Adjust as needed */
  height: auto;
  max-height: 100px;
  object-fit: contain;
  border-radius: 4px;
  flex-shrink: 0;
}

.cinema-info-main {
  flex-grow: 1;
}

.cinema-title-detail {
  font-size: 2rem;
  margin: 0 0 1rem 0;
  font-weight: 600;
  color: #333;
}

.cinema-info-main p {
  margin-bottom: 0.6rem;
  font-size: 1rem;
  color: #555;
  line-height: 1.5;
}

.cinema-info-main strong {
  color: #333;
}

.screenings-section {
  background-color: #fff;
  padding: 2rem;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.screenings-section h2 {
  font-size: 1.6rem;
  margin: 0 0 1.5rem 0;
  padding-bottom: 0.5rem;
  border-bottom: 2px solid #e50914;
  color: #333;
  font-weight: 600;
}

.date-selector {
  margin-bottom: 2rem;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.date-selector label {
  font-weight: 500;
}

.date-selector input[type="date"] {
  padding: 0.5rem;
  border: 1px solid #ccc;
  border-radius: 4px;
}

.movie-groups {
    display: flex;
    flex-direction: column;
    gap: 2.5rem;
}

.movie-group {
  border: 1px solid #eee;
  border-radius: 6px;
  overflow: hidden;
}

.movie-group-header {
    display: flex;
    align-items: center;
    gap: 1rem;
    background-color: #f5f5f5; /* Slightly different background for header */
    padding: 1rem;
    text-decoration: none;
    color: inherit;
    border-bottom: 1px solid #eee;
    transition: background-color 0.2s;
}

.movie-group-header:hover {
    background-color: #e9e9e9;
}

.movie-group-poster {
    width: 60px; /* Smaller poster */
    height: 90px;
    object-fit: cover;
    border-radius: 4px;
    flex-shrink: 0;
}

.movie-group-title {
  font-size: 1.3rem;
  font-weight: 600;
  margin: 0;
  color: #333;
}

.screenings-list {
  list-style: none;
  padding: 0 1rem; /* Padding inside the list */
  margin: 0;
  background-color: #fff;
}

.screening-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem 0;
  border-bottom: 1px dashed #eee; /* Dashed separator */
}

.screening-item:last-child {
  border-bottom: none;
}

.screening-info {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
}

.screening-time {
  font-weight: 600;
  font-size: 1.05rem;
  color: #333;
}

.screening-room {
  color: #666;
  font-size: 0.9rem;
}

.screening-action {
  display: flex;
  align-items: center;
  gap: 1.5rem;
}

.screening-price {
  font-size: 1.1rem;
  font-weight: bold;
  color: #e50914;
}

.btn-buy {
  padding: 0.5rem 1rem;
  background-color: #e50914;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-weight: 600;
  font-size: 0.9rem;
  transition: background-color 0.3s;
}

.btn-buy:hover {
  background-color: #f40612;
}

/* Responsive Adjustments */
@media (max-width: 768px) {
  .cinema-header {
    flex-direction: column;
    align-items: flex-start;
  }
  .cinema-logo-detail {
      margin-bottom: 1rem;
  }
  .cinema-title-detail {
      font-size: 1.8rem;
  }
  .movie-group-title {
      font-size: 1.1rem;
  }
  .screening-item {
      /* Flex-wrap might be better here if space is tight */
      flex-direction: column;
      align-items: flex-start;
      gap: 0.8rem;
  }
  .screening-action {
      width: 100%;
      justify-content: space-between;
      margin-top: 0.5rem;
  }
}

@media (max-width: 480px) {
    .cinema-detail-view, .cinema-header, .screenings-section {
        padding: 1rem;
    }
    .cinema-logo-detail {
        width: 100px;
        max-height: 60px;
    }
    .cinema-title-detail {
        font-size: 1.6rem;
    }
    .movie-group-header {
        padding: 0.8rem;
    }
    .movie-group-poster {
        width: 45px;
        height: 67px;
    }
    .movie-group-title {
        font-size: 1rem;
    }
    .screening-price {
        font-size: 1rem;
    }
    .btn-buy {
        padding: 0.4rem 0.8rem;
    }
}
</style>
