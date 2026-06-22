import { createRouter, createWebHashHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { noAuth: true }
  },
  {
    path: '/',
    component: () => import('@/views/Layout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/Dashboard.vue'),
        meta: { title: '控制台', icon: 'Odometer' }
      },
      {
        path: 'announcements',
        name: 'Announcements',
        component: () => import('@/views/Announcements.vue'),
        meta: { title: '公告管理', icon: 'Bell' }
      },
      {
        path: 'app-versions',
        name: 'AppVersions',
        component: () => import('@/views/AppVersions.vue'),
        meta: { title: 'APP更新', icon: 'Upload' }
      },
      {
        path: 'sources/vod',
        name: 'VodSources',
        component: () => import('@/views/VodSources.vue'),
        meta: { title: '点播源管理', icon: 'VideoCamera' }
      },
      {
        path: 'sources/live',
        name: 'LiveSources',
        component: () => import('@/views/LiveSources.vue'),
        meta: { title: '直播源管理', icon: 'Monitor' }
      },
      {
        path: 'themes',
        name: 'Themes',
        component: () => import('@/views/Themes.vue'),
        meta: { title: '主题管理', icon: 'Brush' }
      },
      {
        path: 'settings',
        name: 'Settings',
        component: () => import('@/views/Settings.vue'),
        meta: { title: '系统设置', icon: 'Setting' }
      },
      {
        path: 'logs',
        name: 'Logs',
        component: () => import('@/views/Logs.vue'),
        meta: { title: '操作日志', icon: 'Document' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const auth = useAuthStore()
  if (to.meta?.noAuth) {
    next()
  } else if (!auth.isLoggedIn()) {
    next('/login')
  } else {
    next()
  }
})

export default router
