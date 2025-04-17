<template>
  <div class="user-profile-container">
    <el-tabs v-model="activeTab" class="profile-tabs">
      <!-- 基本信息 Tab -->
      <el-tab-pane label="基本信息" name="info">
        <el-card class="box-card profile-card">
          <template #header>
            <div class="card-header">
              <span>个人资料</span>
              <el-button
                v-if="!isEditingProfile"
                type="primary"
                :icon="Edit"
                @click="handleEditProfile"
                text
              >
                编辑
              </el-button>
            </div>
          </template>
          <div v-loading="isLoadingProfile" class="profile-content">
            <el-alert
              v-if="profileError"
              :title="profileError"
              type="error"
              show-icon
              :closable="false"
              style="margin-bottom: 20px"
            />

            <!-- 查看模式：使用 ElDescriptions -->
            <el-descriptions
              v-if="userProfile && !isEditingProfile"
              :column="2"
              border
              class="profile-descriptions"
            >
              <el-descriptions-item label="用户名" label-align="right" align="left">{{
                userProfile.username
              }}</el-descriptions-item>
              <el-descriptions-item label="昵称" label-align="right" align="left">{{
                userProfile.nickname || '-'
              }}</el-descriptions-item>
              <el-descriptions-item label="手机号" label-align="right" align="left">{{
                userProfile.phone || '-'
              }}</el-descriptions-item>
              <el-descriptions-item label="邮箱" label-align="right" align="left">{{
                userProfile.email || '-'
              }}</el-descriptions-item>
              <el-descriptions-item label="头像URL" label-align="right" align="left">{{
                userProfile.avatar || '-'
              }}</el-descriptions-item>
              <el-descriptions-item label="角色" label-align="right" align="left">
                <el-tag size="small">{{ userProfile.role }}</el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="注册时间" label-align="right" align="left">
                {{ userProfile.createTime ? formatDateTime(userProfile.createTime) : '-' }}
              </el-descriptions-item>
            </el-descriptions>

            <!-- 编辑模式：使用 ElForm -->
            <el-form
              v-else-if="userProfile && isEditingProfile"
              ref="profileFormRef"
              :model="profileFormData"
              label-width="100px"
              label-position="right"
              class="profile-form edit-mode"
              :rules="profileFormRules"
            >
              <el-form-item label="用户名">
                <span>{{ userProfile.username }}</span>
              </el-form-item>
              <el-form-item label="昵称" prop="nickname">
                <el-input v-model="profileFormData.nickname" placeholder="请输入昵称" />
              </el-form-item>
              <el-form-item label="手机号" prop="phone">
                <el-input v-model="profileFormData.phone" placeholder="请输入手机号" />
              </el-form-item>
              <el-form-item label="邮箱" prop="email">
                <el-input v-model="profileFormData.email" placeholder="请输入邮箱" />
              </el-form-item>
              <el-form-item label="头像URL" prop="avatar">
                <el-input v-model="profileFormData.avatar" placeholder="请输入头像URL" />
              </el-form-item>
              <el-form-item label="角色">
                <el-tag size="small">{{ userProfile.role }}</el-tag>
              </el-form-item>
              <el-form-item label="注册时间">
                <span>{{
                  userProfile.createTime ? formatDateTime(userProfile.createTime) : '-'
                }}</span>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="handleSaveProfile" :loading="isSavingProfile">
                  保存
                </el-button>
                <el-button @click="handleCancelEditProfile">取消</el-button>
              </el-form-item>
            </el-form>

            <el-empty
              v-else-if="!isLoadingProfile && !profileError"
              description="无法加载用户信息"
            />
          </div>
        </el-card>
      </el-tab-pane>

      <!-- 修改密码 Tab -->
      <el-tab-pane label="修改密码" name="password">
        <el-card class="box-card">
          <template #header>
            <div class="card-header">
              <span>修改账户密码</span>
            </div>
          </template>
          <div class="password-content">
            <el-form
              ref="passwordFormRef"
              :model="passwordFormData"
              :rules="passwordFormRules"
              label-width="100px"
              label-position="right"
              class="password-form"
              @submit.prevent="handleChangePassword"
            >
              <el-form-item label="当前密码" prop="currentPassword">
                <el-input
                  type="password"
                  v-model="passwordFormData.currentPassword"
                  show-password
                />
              </el-form-item>
              <el-form-item label="新密码" prop="newPassword">
                <el-input type="password" v-model="passwordFormData.newPassword" show-password />
              </el-form-item>
              <el-form-item label="确认新密码" prop="confirmPassword">
                <el-input
                  type="password"
                  v-model="passwordFormData.confirmPassword"
                  show-password
                />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" native-type="submit" :loading="isChangingPassword">
                  确认修改
                </el-button>
              </el-form-item>
            </el-form>
          </div>
        </el-card>
      </el-tab-pane>

      <!-- 新增我的收藏 Tab -->
      <el-tab-pane label="我的收藏" name="favorites">
        <el-card class="box-card">
          <template #header>
            <div class="card-header">
              <span>我的收藏</span>
            </div>
          </template>
          <div class="favorites-content" v-loading="isLoadingFavorites">
            <el-alert
              v-if="favoritesError"
              :title="favoritesError"
              type="error"
              show-icon
              :closable="false"
              style="margin-bottom: 15px"
            />

            <!-- 无收藏时显示空状态 -->
            <el-empty
              v-if="!isLoadingFavorites && (!favoriteMovies || favoriteMovies.length === 0)"
              description="暂无收藏的电影"
            >
              <el-button type="primary" @click="goToMovieList">去浏览电影</el-button>
            </el-empty>

            <!-- 收藏电影列表 -->
            <div v-else class="favorites-list">
              <el-row :gutter="20">
                <el-col
                  v-for="movie in favoriteMovies"
                  :key="movie.id"
                  :xs="24"
                  :sm="12"
                  :md="8"
                  :lg="6"
                  :xl="4"
                  class="movie-col"
                >
                  <el-card class="movie-card" shadow="hover" :body-style="{ padding: '0px' }">
                    <div class="movie-poster-container">
                      <el-image
                        :src="
                          movie.posterUrl || 'https://via.placeholder.com/300x450.png?text=No+Image'
                        "
                        fit="cover"
                        class="movie-poster"
                        loading="lazy"
                      >
                        <template #error>
                          <div class="poster-error">
                            <el-icon><PictureIcon /></el-icon>
                            <span>加载失败</span>
                          </div>
                        </template>
                      </el-image>
                      <div class="movie-actions">
                        <el-button
                          type="danger"
                          size="small"
                          circle
                          :loading="removingId === movie.id"
                          @click.stop="handleRemoveFavorite(movie.id)"
                          title="取消收藏"
                        >
                          <el-icon><delete /></el-icon>
                        </el-button>
                      </div>
                    </div>
                    <div class="movie-info" @click="goToMovieDetail(movie.id)">
                      <h3 class="movie-title">{{ movie.title }}</h3>
                      <p class="movie-meta">
                        {{ movie.releaseDate ? formatDate(movie.releaseDate) : '未知日期' }}
                      </p>
                      <p class="movie-meta" v-if="movie.movieTypes && movie.movieTypes.length > 0">
                        {{ movie.movieTypes.map((t) => t.name).join(' / ') }}
                      </p>
                    </div>
                  </el-card>
                </el-col>
              </el-row>

              <!-- 分页控件 -->
              <div class="pagination-container">
                <el-pagination
                  v-model:current-page="favoritesCurrentPage"
                  v-model:page-size="favoritesPageSize"
                  :page-sizes="[12, 24, 36, 48]"
                  layout="total, sizes, prev, pager, next"
                  :total="favoritesTotalCount"
                  @size-change="handleFavoritesPageSizeChange"
                  @current-change="handleFavoritesPageChange"
                  :hide-on-single-page="favoritesTotalCount <= favoritesPageSize"
                  background
                />
              </div>
            </div>
          </div>
        </el-card>
      </el-tab-pane>

      <!-- 新增申请合作 Tab，仅对 USER 显示 -->
      <el-tab-pane
        v-if="userProfile && userProfile.role === 'USER'"
        label="申请合作"
        name="apply-cinema"
      >
        <el-card class="box-card">
          <template #header>
            <div class="card-header">
              <span>申请成为影院合作伙伴</span>
            </div>
          </template>
          <!-- 引入申请表单组件 -->
          <CinemaApplicationForm @submit-success="handleApplicationSuccess" />
        </el-card>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive, nextTick, watch } from 'vue'
import {
  ElTabs,
  ElTabPane,
  ElCard,
  ElForm,
  ElFormItem,
  ElInput,
  ElButton,
  ElAlert,
  ElMessage,
  ElEmpty,
  ElRow,
  ElCol,
  ElImage,
  ElPagination,
  ElIcon,
  ElDescriptions,
  ElDescriptionsItem,
  ElTag,
} from 'element-plus'
import { Edit, Delete, Picture as PictureIcon } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import {
  getUserProfile,
  updateUserProfile,
  changePassword,
  getMyFavorites,
  removeFavorite,
} from '@/services/api'
import { useAuthStore } from '@/stores/auth'
import type {
  UserProfileDTO,
  UpdateProfileRequestDTO,
  ChangePasswordRequestDTO,
} from '@/types/auth'
import type { Movie } from '@/types/movie'
import { formatDateTime } from '@/utils/datetime'
import { useRouter } from 'vue-router'
import axios from 'axios'
// 导入新创建的表单组件
import CinemaApplicationForm from '@/components/profile/CinemaApplicationForm.vue'

const router = useRouter()
const authStore = useAuthStore()

// Tab control
const activeTab = ref('info')

// Profile Info State
const isLoadingProfile = ref(true)
const profileError = ref<string | null>(null)
const userProfile = ref<UserProfileDTO | null>(null)
const isEditingProfile = ref(false)
const isSavingProfile = ref(false)
const profileFormRef = ref<FormInstance>()
const profileFormData = reactive<UpdateProfileRequestDTO>({
  nickname: '',
  phone: '',
  email: '',
  avatar: '',
})

// Add validation rules for profile form
const profileFormRules = reactive<FormRules>({
  phone: [
    {
      pattern: /^1[3-9]\d{9}$/,
      message: '请输入有效的中国大陆手机号码',
      trigger: ['blur', 'change'],
    },
  ],
  email: [{ type: 'email', message: '请输入有效的邮箱地址', trigger: ['blur', 'change'] }],
})

// Password Change State
const isChangingPassword = ref(false)
const passwordFormRef = ref<FormInstance>()
const passwordFormData = reactive<ChangePasswordRequestDTO & { confirmPassword?: string }>({
  currentPassword: '',
  newPassword: '',
  confirmPassword: '',
})

// --- 我的收藏电影状态 ---
const isLoadingFavorites = ref(false)
const favoritesError = ref<string | null>(null)
const favoriteMovies = ref<Movie[]>([])
const favoritesTotalCount = ref(0)
const favoritesCurrentPage = ref(1)
const favoritesPageSize = ref(12)
const removingId = ref<number | null>(null)

// 格式化日期函数（仅显示年月日）
const formatDate = (dateString: string) => {
  if (!dateString) return '未知日期'
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

// --- Fetch User Profile Logic ---
const fetchUserProfile = async () => {
  isLoadingProfile.value = true
  profileError.value = null
  try {
    const response = await getUserProfile()
    userProfile.value = response.data
    // Initialize form data when profile loads
    resetProfileFormData()
  } catch (err) {
    console.error('Failed to fetch user profile:', err)
    if (axios.isAxiosError(err) && err.response?.status === 401) {
      profileError.value = '请先登录以查看个人信息。' // More specific message for 401
    } else {
      profileError.value = '加载用户信息失败，请稍后重试。'
    }
    userProfile.value = null
  } finally {
    isLoadingProfile.value = false
  }
}

onMounted(() => {
  fetchUserProfile()
  // 如果初始选中的是收藏标签页，则加载收藏数据
  if (activeTab.value === 'favorites') {
    fetchFavoriteMovies()
  }
})

// --- Profile Edit Logic ---
const resetProfileFormData = () => {
  if (userProfile.value) {
    profileFormData.nickname = userProfile.value.nickname || ''
    profileFormData.phone = userProfile.value.phone || ''
    profileFormData.email = userProfile.value.email || ''
    profileFormData.avatar = userProfile.value.avatar || ''
  } else {
    Object.assign(profileFormData, { nickname: '', phone: '', email: '', avatar: '' })
  }
  // Reset validation state when resetting form
  nextTick(() => {
    profileFormRef.value?.clearValidate()
  })
}

const handleEditProfile = () => {
  isEditingProfile.value = true
  resetProfileFormData()
}

const handleCancelEditProfile = () => {
  isEditingProfile.value = false
  resetProfileFormData()
}

const handleSaveProfile = async () => {
  if (!profileFormRef.value) return
  // Validate the form before submitting
  await profileFormRef.value.validate(async (valid) => {
    if (valid) {
      isSavingProfile.value = true
      try {
        const response = await updateUserProfile(profileFormData)
        userProfile.value = response.data
        if (authStore.user) {
          // Update nickname and potentially other fields if needed
          authStore.user.nickname = response.data.nickname
          // Consider updating phone/email in store too if they are used elsewhere reactively
          // authStore.user.phone = response.data.phone; // Example
          // authStore.user.email = response.data.email; // Example
        } else {
          console.warn('Attempted to update store, but authStore.user was null.')
        }
        isEditingProfile.value = false
        ElMessage.success('个人信息更新成功！')
      } catch (err) {
        console.error('Failed to update profile:', err)
        if (axios.isAxiosError(err) && err.response) {
          if (err.response.status === 409) {
            ElMessage.error('更新失败：手机号或邮箱已被占用。')
          } else if (err.response.status === 400) {
            // More specific message for validation errors
            ElMessage.error(`更新失败：${err.response.data || '请检查输入格式或内容。'}`)
          } else if (err.response.status === 403) {
            // Keep the 403 message just in case, though 400 is more likely the root
            ElMessage.error('更新失败：权限不足或操作被禁止。')
          } else {
            ElMessage.error('更新失败，请稍后重试。')
          }
        } else {
          ElMessage.error('更新失败，请稍后重试。')
        }
      } finally {
        isSavingProfile.value = false
      }
    } else {
      console.log('Profile form validation failed')
      ElMessage.warning('请检查输入的信息是否符合要求。') // Notify user about validation failure
      return // Stop submission if validation fails
    }
  })
}

// --- Change Password Logic ---

// Password validation rules
const validatePassConfirm = (rule: unknown, value: string, callback: (error?: Error) => void) => {
  if (value === '') {
    callback(new Error('请再次输入新密码'))
  } else if (value !== passwordFormData.newPassword) {
    callback(new Error('两次输入的新密码不一致!'))
  } else {
    callback()
  }
}

const passwordFormRules = reactive<FormRules>({
  currentPassword: [{ required: true, message: '请输入当前密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 100, message: '密码长度应为 6 到 100 个字符', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    { validator: validatePassConfirm, trigger: 'blur' },
  ],
})

const handleChangePassword = async () => {
  if (!passwordFormRef.value) return
  await passwordFormRef.value.validate(async (valid) => {
    if (valid) {
      isChangingPassword.value = true
      try {
        // Prepare data for API (exclude confirmPassword from the reactive form if not needed, but API expects it)
        const apiData: ChangePasswordRequestDTO = {
          currentPassword: passwordFormData.currentPassword,
          newPassword: passwordFormData.newPassword,
          // Correctly send the newPassword as confirmPassword for backend validation
          confirmPassword: passwordFormData.newPassword,
        }
        await changePassword(apiData)
        ElMessage.success('密码修改成功！')
        passwordFormRef.value?.resetFields() // Reset form fields
      } catch (err) {
        console.error('Failed to change password:', err)
        if (axios.isAxiosError(err) && err.response) {
          if (err.response.status === 400) {
            ElMessage.error(
              `密码修改失败：${err.response.data || '请检查输入。可能是当前密码错误或新密码格式不正确。'}`,
            )
          } else if (err.response.status === 401) {
            ElMessage.error('密码修改失败：当前密码不正确。') // 401 might also indicate wrong current pwd
          } else {
            ElMessage.error('密码修改失败，请稍后重试。')
          }
        } else {
          ElMessage.error('密码修改失败，请稍后重试。')
        }
      } finally {
        isChangingPassword.value = false
      }
    }
  })
}

// 加载收藏电影列表
const fetchFavoriteMovies = async () => {
  isLoadingFavorites.value = true
  favoritesError.value = null
  try {
    const response = await getMyFavorites({
      current: favoritesCurrentPage.value,
      size: favoritesPageSize.value,
    })
    favoriteMovies.value = response.data.records || []
    favoritesTotalCount.value = response.data.total || 0
  } catch (err) {
    console.error('Failed to fetch favorite movies:', err)
    if (axios.isAxiosError(err) && err.response?.status === 401) {
      favoritesError.value = '请先登录以查看收藏信息。'
    } else {
      favoritesError.value = '加载收藏电影失败，请稍后重试。'
    }
    favoriteMovies.value = []
    favoritesTotalCount.value = 0
  } finally {
    isLoadingFavorites.value = false
  }
}

// 取消收藏电影
const handleRemoveFavorite = async (movieId: number) => {
  removingId.value = movieId
  try {
    await removeFavorite(movieId)
    ElMessage.success('已取消收藏')
    // 刷新收藏列表
    await fetchFavoriteMovies()
  } catch (err) {
    console.error('Failed to remove favorite:', err)
    ElMessage.error('取消收藏失败，请稍后重试')
  } finally {
    removingId.value = null
  }
}

// 分页处理
const handleFavoritesPageChange = (page: number) => {
  favoritesCurrentPage.value = page
  fetchFavoriteMovies()
}

const handleFavoritesPageSizeChange = (size: number) => {
  favoritesPageSize.value = size
  favoritesCurrentPage.value = 1 // 重置到第一页
  fetchFavoriteMovies()
}

// 导航到电影详情页
const goToMovieDetail = (movieId: number) => {
  router.push(`/movies/${movieId}`)
}

// 导航到电影列表页
const goToMovieList = () => {
  router.push('/movies')
}

// 申请成功后的处理（可选）
const handleApplicationSuccess = () => {
  // 可以在这里做一些操作，比如提示用户、或者刷新用户信息（虽然当前申请不直接改变用户信息）
  console.log('影院申请提交成功！')
  // 可以考虑禁用申请tab或显示已申请状态，但这需要后端提供查询申请状态的接口
}

// 监听 tab 变化，当切换到收藏标签页时加载数据
watch(activeTab, (newTab) => {
  if (newTab === 'favorites') {
    fetchFavoriteMovies()
  }
})
</script>

<style scoped>
.user-profile-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.profile-tabs {
  width: 100%;
}

.profile-card .el-card__body {
  min-height: 200px; /* Give some minimum height */
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.profile-content {
  /* 添加一些内边距，使其不紧贴卡片边缘 */
  padding: 10px 0;
}

/* Descriptions 样式优化 */
.profile-descriptions {
  margin-top: 10px; /* 与上方内容保持间距 */
}

.profile-descriptions :deep(.el-descriptions__label) {
  /* 调整标签宽度或样式 */
  /* min-width: 100px; */
}

/* 编辑表单样式调整 */
.profile-form.edit-mode {
  max-width: 500px;
  margin: 10px auto 0; /* 顶部加一点间距 */
}

.password-form {
  max-width: 500px;
  margin: 0 auto;
}

/* 新增收藏电影列表的样式 */
.favorites-list {
  margin-top: 20px;
}

.movie-col {
  margin-bottom: 20px;
}

.movie-card {
  transition: transform 0.3s;
  height: 100%;
  overflow: hidden;
}

.movie-card:hover {
  transform: translateY(-5px);
}

.movie-poster-container {
  position: relative;
  padding-top: 150%; /* 2:3比例的海报 */
  overflow: hidden;
}

.movie-poster {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.movie-actions {
  position: absolute;
  top: 10px;
  right: 10px;
  display: none;
}

.movie-poster-container:hover .movie-actions {
  display: block;
}

.movie-info {
  padding: 15px;
  cursor: pointer;
}

.movie-title {
  font-size: 16px;
  margin: 0 0 8px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.movie-meta {
  font-size: 13px;
  color: #999;
  margin: 3px 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.poster-error {
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  color: #909399;
  background-color: #f5f7fa;
}

.pagination-container {
  margin-top: 30px;
  display: flex;
  justify-content: center;
}

/* 响应式调整 */
@media (max-width: 768px) {
  .movie-col {
    width: 50%;
  }
}

@media (max-width: 480px) {
  .movie-col {
    width: 100%;
  }
}
</style>
