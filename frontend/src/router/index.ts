import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import AdminLayout from '@/layouts/AdminLayout.vue'
import { useAuthStore } from '@/stores/auth'
import CinemaAdminLayout from '@/layouts/CinemaAdminLayout.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomeView,
    },
    {
      path: '/login',
      name: 'login',
      component: () => import('../views/LoginView.vue'),
    },
    {
      path: '/register',
      name: 'register',
      component: () => import('../views/RegisterView.vue'),
    },
    {
      path: '/movies',
      name: 'movie-list',
      component: () => import('../views/MovieListView.vue'),
    },
    {
      path: '/movies/:id',
      name: 'movie-detail',
      component: () => import('../views/MovieDetailView.vue'),
      props: true,
    },
    {
      path: '/cinemas',
      name: 'cinema-list',
      component: () => import('../views/CinemaListView.vue'),
    },
    {
      path: '/cinemas/:id',
      name: 'cinema-detail',
      component: () => import('../views/CinemaDetailView.vue'),
      props: true,
    },
    {
      path: '/about',
      name: 'about',
      // route level code-splitting
      // this generates a separate chunk (About.[hash].js) for this route
      // which is lazy-loaded when the route is visited.
      component: () => import('../views/AboutView.vue'),
    },
    {
      path: '/admin',
      component: AdminLayout,
      meta: { requiresAuth: true, requiredRole: 'SYSTEM_ADMIN' },
      children: [
        {
          path: '',
          redirect: '/admin/dashboard',
        },
        {
          path: 'dashboard',
          name: 'admin-dashboard',
          component: () => import('@/views/admin/DashboardView.vue'),
        },
        {
          path: 'users',
          name: 'admin-users',
          component: () => import('@/views/admin/UserManagementView.vue'),
        },
        {
          path: 'movies',
          name: 'admin-movies',
          component: () => import('@/views/admin/MovieManagementView.vue'),
        },
        {
          path: 'cinemas',
          name: 'admin-cinemas',
          component: () => import('@/views/admin/CinemaManagementView.vue'),
        },
        {
          path: 'screenings',
          name: 'admin-screenings',
          component: () => import('@/views/admin/ScreeningManagementView.vue'),
        },
        {
          path: 'announcements',
          name: 'admin-announcements',
          component: () => import('@/views/admin/AnnouncementManagementView.vue'),
        },
      ],
    },
    {
      path: '/cinema-admin',
      component: CinemaAdminLayout,
      meta: { requiresAuth: true, requiredRole: 'CINEMA_ADMIN' },
      children: [
        {
          path: '',
          redirect: '/cinema-admin/my-cinema',
        },
        {
          path: 'my-cinema',
          name: 'cinema-admin-my-cinema',
          component: () => import('@/views/cinema-admin/MyCinemaView.vue'),
        },
        {
          path: 'rooms',
          name: 'cinema-admin-rooms',
          component: () => import('@/views/cinema-admin/RoomManagementView.vue'),
        },
        {
          path: 'screenings',
          name: 'cinema-admin-screenings',
          component: () => import('@/views/cinema-admin/ScreeningManagementView.vue'),
        },
        {
          path: 'orders',
          name: 'cinema-admin-orders',
          component: () => import('@/views/cinema-admin/OrdersManagementView.vue'),
        },
      ],
    },
    {
      path: '/orders/:identifier',
      name: 'OrderDetail',
      component: () => import('@/views/OrderDetailView.vue'),
      props: true,
      meta: { requiresAuth: true },
    },
    {
      path: '/orders',
      name: 'OrdersList',
      component: () => import('@/views/OrdersListView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/profile',
      name: 'profile',
      component: () => import('@/views/UserProfileView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/book/:screeningId',
      name: 'seat-selection',
      component: () => import('@/views/SeatSelectionView.vue'),
      props: (route) => ({ screeningId: Number(route.params.screeningId) }),
      meta: { requiresAuth: true },
    },
  ],
})

// --- Add Navigation Guard Start ---
router.beforeEach(async (to, from, next) => {
  const authStore = useAuthStore()

  // Ensure user info is potentially loaded if needed (adjust based on your store logic)
  // Example: If user info isn't loaded on app start, you might need to await authStore.fetchUserInfo()
  // Ensure this is handled correctly to avoid infinite loops, especially around login pages.

  const requiresAuth = to.matched.some((record) => record.meta.requiresAuth)
  const requiredRole = to.meta.requiredRole as string | undefined

  // If route requires authentication
  if (requiresAuth) {
    // Check if user is logged in
    if (!authStore.isLoggedIn) {
      // Redirect to login page, preserving the intended destination
      next({ name: 'login', query: { redirect: to.fullPath } })
    } else {
      // Check if a specific role is required
      if (requiredRole) {
        // Check if user has the required role
        if (authStore.user?.role === requiredRole) {
          // User has the required role, proceed
          next()
        } else {
          // User does not have the required role, redirect to home or an unauthorized page
          console.warn(
            `Unauthorized access attempt to ${to.path} by user with role ${authStore.user?.role || 'unknown'}. Required role: ${requiredRole}`,
          )
          // You might want to show a notification here
          next({ name: 'home' }) // Or create an 'unauthorized' page
        }
      } else {
        // Route requires auth, but no specific role, proceed
        next()
      }
    }
  } else {
    // Route does not require authentication, proceed
    next()
  }
})
// --- Add Navigation Guard End ---

export default router
