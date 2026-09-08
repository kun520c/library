import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

declare module 'vue-router' {
  interface RouteMeta {
    title?: string
    guest?: boolean
    requiresAdmin?: boolean
  }
}

const routes: RouteRecordRaw[] = [
  { path: '/login', component: () => import('@/views/auth/LoginView.vue'), meta: { title: '登录', guest: true } },
  { path: '/register', component: () => import('@/views/auth/RegisterView.vue'), meta: { title: '注册', guest: true } },
  {
    path: '/',
    component: () => import('@/layouts/AppLayout.vue'),
    children: [
      { path: '', redirect: '/library' },
      { path: 'library', component: () => import('@/views/library/LibraryView.vue'), meta: { title: '书库' } },
      { path: 'library/:id', component: () => import('@/views/library/BookDetailView.vue'), meta: { title: '图书详情' } },
      { path: 'borrow', component: () => import('@/views/borrow/BorrowListView.vue'), meta: { title: '我的借阅' } },
      { path: 'borrow/:id', component: () => import('@/views/borrow/BorrowDetailView.vue'), meta: { title: '借阅详情' } },
      { path: 'profile', component: () => import('@/views/ProfileView.vue'), meta: { title: '个人资料' } },
      { path: 'admin', component: () => import('@/views/admin/AdminOverviewView.vue'), meta: { title: '管理概览', requiresAdmin: true } },
      { path: 'admin/books', component: () => import('@/views/admin/AdminBooksView.vue'), meta: { title: '图书管理', requiresAdmin: true } },
      { path: 'admin/categories', component: () => import('@/views/admin/AdminCategoriesView.vue'), meta: { title: '分类管理', requiresAdmin: true } },
      { path: 'admin/users', component: () => import('@/views/admin/AdminUsersView.vue'), meta: { title: '用户查询', requiresAdmin: true } },
      { path: 'admin/borrows', component: () => import('@/views/admin/AdminBorrowsView.vue'), meta: { title: '借阅管理', requiresAdmin: true } },
    ],
  },
  { path: '/:pathMatch(.*)*', component: () => import('@/views/NotFoundView.vue'), meta: { title: '页面未找到', guest: true } },
]

const router = createRouter({ history: createWebHistory(), routes, scrollBehavior: () => ({ top: 0 }) })

router.beforeEach(async (to) => {
  const auth = useAuthStore()
  await auth.bootstrap()
  document.title = `${to.meta.title || '书库'} · 一方书室`
  if (to.meta.guest) {
    if (auth.isAuthenticated && ['/login', '/register'].includes(to.path)) {
      return auth.isAdmin ? '/admin' : '/library'
    }
    return true
  }
  if (!auth.isAuthenticated) return { path: '/login', query: { redirect: to.fullPath } }
  if (to.meta.requiresAdmin && !auth.isAdmin) return '/library'
  return true
})

export default router
