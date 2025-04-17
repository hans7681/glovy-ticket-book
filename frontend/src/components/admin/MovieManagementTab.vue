<template>
  <div class="movie-management-tab">
    <!-- Filter Section -->
    <el-card class="filter-card" shadow="never">
      <el-form :inline="true" :model="movieFilters" class="filter-form">
        <el-form-item label="标题">
          <el-input
            v-model="movieFilters.title"
            placeholder="按标题搜索"
            style="width: 220px"
          ></el-input>
        </el-form-item>
        <el-form-item label="状态">
          <el-select
            v-model="movieFilters.status"
            placeholder="按状态筛选"
            clearable
            style="width: 180px"
          >
            <el-option label="即将上映" value="COMING_SOON"></el-option>
            <el-option label="正在热映" value="NOW_PLAYING"></el-option>
            <el-option label="已下线" value="OFFLINE"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="类型">
          <el-select
            v-model="movieFilters.movieTypeId"
            placeholder="按类型筛选"
            clearable
            filterable
            style="width: 200px"
          >
            <el-option
              v-for="type in movieTypesForFilter"
              :key="type.id"
              :label="type.name"
              :value="type.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleMovieFilter">
            <el-icon><Search /></el-icon> 查询
          </el-button>
          <el-button @click="resetMovieFilters">
            <el-icon><Refresh /></el-icon> 重置
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card" shadow="never">
      <div class="table-header">
        <div class="table-title">电影列表</div>
        <div class="actions">
          <el-button type="primary" @click="handleAddMovie">
            <el-icon><Plus /></el-icon> 新增电影
          </el-button>
        </div>
      </div>

      <!-- Table Section -->
      <el-table :data="movies" v-loading="movieLoading" style="width: 100%" stripe border>
        <el-table-column prop="id" label="ID" width="80" align="center"></el-table-column>
        <el-table-column
          prop="title"
          label="标题"
          min-width="180"
          show-overflow-tooltip
        ></el-table-column>
        <el-table-column
          prop="director"
          label="导演"
          min-width="120"
          show-overflow-tooltip
        ></el-table-column>
        <el-table-column label="上映日期" width="120" align="center">
          <template #default="{ row }">
            {{ row.releaseDate || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="getMovieStatusTagType(row.status)">{{
              getMovieStatusText(row.status)
            }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right" align="center">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click="handleEditMovie(row)">
              <el-icon><Edit /></el-icon> 编辑
            </el-button>
            <el-button size="small" type="danger" @click="handleDeleteMovie(row.id)">
              <el-icon><Delete /></el-icon> 删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- Pagination -->
      <el-pagination
        v-if="moviePagination.totalItems > 0"
        background
        layout="total, sizes, prev, pager, next, jumper"
        :total="moviePagination.totalItems"
        :current-page="moviePagination.currentPage"
        :page-size="moviePagination.pageSize"
        :page-sizes="[10, 20, 50, 100]"
        @current-change="handleMoviePageChange"
        @size-change="handleMovieSizeChange"
        class="pagination-container"
      />
    </el-card>

    <!-- Add/Edit Movie Dialog -->
    <el-dialog
      v-model="movieDialogVisible"
      :title="currentMovie.id ? '编辑电影' : '新增电影'"
      width="70%"
      top="5vh"
      @close="resetMovieForm"
      center
    >
      <el-form
        ref="movieFormRef"
        :model="currentMovie"
        :rules="movieFormRules"
        label-width="100px"
        label-position="top"
      >
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="标题" prop="title">
              <el-input v-model="currentMovie.title" placeholder="请输入电影标题"></el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="导演" prop="director">
              <el-input v-model="currentMovie.director" placeholder="请输入导演姓名"></el-input>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="演员" prop="actors">
          <el-input
            type="textarea"
            :rows="2"
            v-model="currentMovie.actors"
            placeholder="请输入主要演员，用逗号分隔"
          ></el-input>
        </el-form-item>

        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="时长 (分钟)" prop="duration">
              <el-input
                type="number"
                v-model.number="currentMovie.duration"
                placeholder="请输入电影时长"
              ></el-input>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="上映日期" prop="releaseDate">
              <el-date-picker
                v-model="currentMovie.releaseDate"
                type="date"
                placeholder="选择上映日期"
                value-format="YYYY-MM-DD"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="国家/地区" prop="country">
              <el-input v-model="currentMovie.country" placeholder="请输入制片国家/地区"></el-input>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="简介" prop="description">
          <el-input
            type="textarea"
            :rows="4"
            v-model="currentMovie.description"
            placeholder="请输入电影简介"
          ></el-input>
        </el-form-item>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="海报 URL" prop="posterUrl">
              <el-input
                v-model="currentMovie.posterUrl"
                placeholder="请输入海报图片 URL"
              ></el-input>
              <!-- TODO: Add image preview or upload functionality later -->
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="预告片 URL" prop="trailerUrl">
              <el-input
                v-model="currentMovie.trailerUrl"
                placeholder="请输入预告片 URL (可选)"
              ></el-input>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="状态" prop="status">
              <el-select
                v-model="currentMovie.status"
                placeholder="选择电影状态"
                style="width: 100%"
              >
                <el-option label="即将上映" value="COMING_SOON"></el-option>
                <el-option label="正在热映" value="NOW_PLAYING"></el-option>
                <el-option label="已下线" value="OFFLINE"></el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="电影类型" prop="movieTypeIds">
              <el-select
                v-model="currentMovie.movieTypeIds"
                multiple
                filterable
                placeholder="请选择电影类型"
                style="width: 100%"
              >
                <el-option
                  v-for="type in movieTypesForForm"
                  :key="type.id"
                  :label="type.name"
                  :value="type.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="movieDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitMovieForm">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import {
  ElForm,
  ElFormItem,
  ElInput,
  ElSelect,
  ElOption,
  ElButton,
  ElTable,
  ElTableColumn,
  ElPagination,
  ElDialog,
  ElTag,
  ElDatePicker,
  ElRow,
  ElCol,
  ElMessageBox,
  ElMessage,
  ElCard,
  ElIcon,
  type FormInstance,
  type FormRules,
} from 'element-plus'
import { Edit, Delete, Search, Refresh, Plus } from '@element-plus/icons-vue'
import {
  adminListMovies, // Use admin specific list if available, else listMovies
  addMovie,
  updateMovie,
  deleteMovie,
  getMovieTypes, // Fetch types for filter and form
  getMovieById, // Fetch full details for editing
} from '@/services/api'
import type { Movie } from '@/types/movie'
import type { MovieType } from '@/types/movieType'
import axios from 'axios'

// Movie State
const movies = ref<Movie[]>([])
const movieLoading = ref(true)
const movieDialogVisible = ref(false)
const movieFormRef = ref<FormInstance>()
const moviePagination = reactive({
  currentPage: 1,
  pageSize: 10,
  totalItems: 0,
})
const movieFilters = reactive({
  title: '',
  status: undefined as 'COMING_SOON' | 'NOW_PLAYING' | 'OFFLINE' | undefined,
  movieTypeId: undefined as number | undefined,
})
const currentMovie = reactive<Partial<Movie>>({}) // Use Partial for form

// Movie Type State (for filter and form)
const allMovieTypes = ref<MovieType[]>([])

// Computed properties for select options
const movieTypesForFilter = computed(() => allMovieTypes.value)
const movieTypesForForm = computed(() => allMovieTypes.value)

// Movie Form Rules
const movieFormRules = reactive<FormRules>({
  title: [{ required: true, message: '请输入电影标题', trigger: 'blur' }],
  status: [{ required: true, message: '请选择电影状态', trigger: 'change' }],
  movieTypeIds: [
    { required: true, message: '请至少选择一个电影类型', trigger: 'change', type: 'array' },
    // Optional: Custom validator for max 3 types if multiple-limit isn't enough
    //   { validator: (rule, value, callback) => {
    //       if (value && value.length > 3) {
    //         callback(new Error('最多只能选择3个类型'));
    //       } else {
    //         callback();
    //       }
    //     }, trigger: 'change' }
  ],
  releaseDate: [{ type: 'date', message: '请选择有效的上映日期', trigger: 'change' }],
  // Add more rules as needed for director, actors, duration etc.
  duration: [
    {
      type: 'number',
      message: '时长必须是数字',
      trigger: 'blur',
      transform: (value) => Number(value),
    },
  ],
})

// --- Fetch Data ---
const fetchMovieTypes = async () => {
  try {
    const response = await getMovieTypes()
    allMovieTypes.value = response.data
  } catch (error) {
    console.error('Failed to fetch movie types for movie tab:', error)
    // Handle error appropriately, maybe show a message
  }
}

const fetchMovies = async () => {
  movieLoading.value = true
  try {
    const params = {
      current: moviePagination.currentPage,
      size: moviePagination.pageSize,
      title: movieFilters.title || undefined,
      status: movieFilters.status || undefined,
      movieTypeId: movieFilters.movieTypeId || undefined,
    }
    const response = await adminListMovies(params)
    movies.value = response.data.records || []
    moviePagination.totalItems = response.data.total || 0
  } catch (error) {
    console.error('Failed to fetch movies:', error)
    ElMessage.error('加载电影列表失败')
    movies.value = []
    moviePagination.totalItems = 0
  } finally {
    movieLoading.value = false
  }
}

onMounted(() => {
  fetchMovieTypes() // Fetch types first
  fetchMovies() // Then fetch movies
})

// --- Filtering and Pagination ---
const handleMovieFilter = () => {
  moviePagination.currentPage = 1 // Reset page when filtering
  fetchMovies()
}

const resetMovieFilters = () => {
  movieFilters.title = ''
  movieFilters.status = undefined
  movieFilters.movieTypeId = undefined
  fetchMovies() // Refetch with default filters
}

const handleMoviePageChange = (page: number) => {
  moviePagination.currentPage = page
  fetchMovies()
}

const handleMovieSizeChange = (size: number) => {
  moviePagination.pageSize = size
  moviePagination.currentPage = 1 // Reset page
  fetchMovies()
}

// --- CRUD Operations ---

const resetMovieForm = () => {
  // Reset reactive object to initial state
  Object.keys(currentMovie).forEach((key) => delete currentMovie[key as keyof Movie])
  // Explicitly set fields expected by the form to default values
  currentMovie.title = ''
  currentMovie.director = ''
  currentMovie.actors = ''
  currentMovie.duration = undefined
  currentMovie.description = ''
  currentMovie.posterUrl = ''
  currentMovie.releaseDate = undefined
  currentMovie.country = ''
  currentMovie.status = undefined
  currentMovie.trailerUrl = ''
  currentMovie.movieTypeIds = [] // Important: reset movieTypeIds

  movieFormRef.value?.clearValidate() // Clear validation
}

const handleAddMovie = () => {
  resetMovieForm()
  movieDialogVisible.value = true
}

const handleEditMovie = async (movie: Movie) => {
  resetMovieForm() // Start fresh
  try {
    // Fetch the full movie details
    const response = await getMovieById(movie.id)
    const movieDetails = response.data

    // Assign fetched data to currentMovie
    Object.assign(currentMovie, movieDetails)

    // Extract type IDs from the movieTypes array if it exists
    if (Array.isArray(movieDetails.movieTypes)) {
      currentMovie.movieTypeIds = movieDetails.movieTypes.map((type) => type.id)
    } else {
      // Fallback or if movieTypes is not present/null
      currentMovie.movieTypeIds = []
    }

    // Ensure movieTypeIds is always an array (redundant now but safe)
    // if (!currentMovie.movieTypeIds) {
    //   currentMovie.movieTypeIds = [];
    // }

    movieDialogVisible.value = true
  } catch (error) {
    console.error(`Failed to fetch movie details for ID ${movie.id}:`, error)
    ElMessage.error('获取电影详情失败，无法编辑。')
    // Fallback: Use list data, but movieTypeIds might be missing
    // Object.assign(currentMovie, { ...movie, movieTypeIds: movie.movieTypeIds || [] });
    // movieDialogVisible.value = true; // Optionally still open dialog with possibly incomplete data
  }
}

const handleDeleteMovie = async (id: number) => {
  try {
    await ElMessageBox.confirm('确定要删除这部电影吗？此操作不可恢复。', '确认删除', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await deleteMovie(id)
    ElMessage.success('电影删除成功')
    await fetchMovies() // Refresh list
  } catch (error) {
    if (error === 'cancel') {
      ElMessage.info('已取消删除')
      return
    }
    console.error('Failed to delete movie:', error)
    if (axios.isAxiosError(error) && error.response?.status === 400) {
      ElMessage.error(`删除失败: ${error.response.data || '该电影可能有关联场次数据'}`)
    } else {
      ElMessage.error('删除电影失败')
    }
  }
}

const submitMovieForm = async () => {
  if (!movieFormRef.value) return
  await movieFormRef.value.validate(async (valid) => {
    if (valid) {
      // Prepare data, ensuring required fields are present
      const movieDataToSend: Omit<Movie, 'id' | 'createTime' | 'updateTime' | 'movieTypes'> & {
        id?: number
        movieTypeIds: number[]
      } = {
        // Copy relevant fields from currentMovie
        title: currentMovie.title || '',
        director: currentMovie.director,
        actors: currentMovie.actors,
        duration: currentMovie.duration,
        description: currentMovie.description,
        posterUrl: currentMovie.posterUrl,
        releaseDate: currentMovie.releaseDate,
        country: currentMovie.country,
        status: currentMovie.status || 'COMING_SOON',
        trailerUrl: currentMovie.trailerUrl,
        movieTypeIds: currentMovie.movieTypeIds || [],
        // Include id only if updating
        ...(currentMovie.id && { id: currentMovie.id }),
      }

      try {
        if (currentMovie.id) {
          // Update: Send the object including the id
          await updateMovie(currentMovie.id, movieDataToSend as Movie) // Assert back to Movie for update
          ElMessage.success('电影更新成功')
        } else {
          // Add: Send the object which naturally excludes the id because it wasn't added above
          await addMovie(movieDataToSend as Movie) // Assert back to Movie for add
          ElMessage.success('电影添加成功')
        }
        movieDialogVisible.value = false
        await fetchMovies() // Refresh list
      } catch (error) {
        console.error('Failed to save movie:', error)
        ElMessage.error('保存电影失败')
        // Add more specific error handling based on status codes if needed
      }
    }
  })
}

// --- UI Helper Functions ---
const getMovieStatusTagType = (status?: string) => {
  switch (status) {
    case 'COMING_SOON':
      return 'warning'
    case 'NOW_PLAYING':
      return 'success'
    case 'OFFLINE':
      return 'info'
    default:
      return 'info'
  }
}

const getMovieStatusText = (status?: string) => {
  switch (status) {
    case 'COMING_SOON':
      return '即将上映'
    case 'NOW_PLAYING':
      return '正在热映'
    case 'OFFLINE':
      return '已下线'
    default:
      return '未知'
  }
}
</script>

<style scoped>
.movie-management-tab {
  height: auto;
  overflow: visible;
  width: 100%;
}

.filter-card {
  margin-bottom: 20px;
  height: auto;
  overflow: visible;
  width: 100%;
}

.filter-form .el-form-item {
  margin-bottom: 10px;
  margin-right: 10px;
}

.table-card {
  margin-bottom: 20px;
  height: auto;
  overflow: visible;
  width: 100%;
}

/* 控制表格组件的滚动行为 */
:deep(.el-table) {
  width: 100% !important;
  overflow: visible;
}

:deep(.el-table__body-wrapper) {
  overflow: visible;
}

:deep(.el-card__body) {
  overflow: visible !important;
  padding: 15px;
}

.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.table-title {
  font-size: 16px;
  font-weight: bold;
  color: #303133;
}

.actions {
  display: flex;
  justify-content: flex-end;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.dialog-footer {
  width: 100%;
  display: flex;
  justify-content: flex-end;
}
</style>
