import { createRouter, createWebHashHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    redirect: '/dashboard',
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/Register.vue'),
  },
  {
    path: '/dashboard',
    name: 'Dashboard',
    component: () => import('@/views/Dashboard.vue'),
  },
  {
    path: '/recommend',
    name: 'Recommend',
    component: () => import('@/views/Recommend.vue'),
  },
  {
    path: '/item-recommend',
    name: 'ItemRecommend',
    component: () => import('@/views/ItemRecommend.vue'),
  },
  {
    path: '/analytics',
    name: 'Analytics',
  component: () => import('@/views/Analytics.vue'),
  },
]

 const router = createRouter({
   history: createWebHashHistory(),
  routes,
})

// Simple auth guard
router.beforeEach((to, _from, next) => {
  const isAuthPage = ['/login', '/register'].includes(to.path)
  const token = localStorage.getItem('token')
  if (!token && !isAuthPage) {
    next('/login')
  } else if (token && isAuthPage) {
    next('/dashboard')
  } else {
    next()
  }
})

export default router
