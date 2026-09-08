<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowDown, Collection, Fold, House, Notebook, Reading, User, UserFilled } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()
const mobileOpen = ref(false)
const title = computed(() => route.meta.title || '一方书室')

const userNav = [
  { to: '/library', label: '书库', icon: Reading },
  { to: '/borrow', label: '我的借阅', icon: Reading },
  { to: '/profile', label: '个人资料', icon: User },
]
const adminNav = [
  { to: '/admin', label: '管理概览', icon: House },
  { to: '/admin/books', label: '图书管理', icon: Notebook },
  { to: '/admin/categories', label: '分类管理', icon: Collection },
  { to: '/admin/users', label: '用户查询', icon: UserFilled },
  { to: '/admin/borrows', label: '借阅管理', icon: Reading },
]

function isActive(path: string) {
  return path === '/admin' ? route.path === path : route.path.startsWith(path)
}

function logout() {
  auth.logout()
  router.replace('/login')
}
</script>

<template>
  <div class="app-shell">
    <aside class="sidebar" :class="{ open: mobileOpen }">
      <RouterLink to="/library" class="sidebar-brand" @click="mobileOpen = false">
        <span class="brand-mark brand-mark--light" aria-hidden="true"><i /><i /><i /></span>
        <span><b>一方书室</b><small>LIBRARY DESK</small></span>
      </RouterLink>
      <nav class="side-nav" aria-label="主导航">
        <p class="nav-caption">阅读</p>
        <RouterLink
          v-for="item in userNav" :key="item.to" :to="item.to"
          :class="{ active: isActive(item.to) }" @click="mobileOpen = false"
        >
          <el-icon><component :is="item.icon" /></el-icon><span>{{ item.label }}</span>
        </RouterLink>
        <template v-if="auth.isAdmin">
          <p class="nav-caption nav-caption--spaced">管理</p>
          <RouterLink
            v-for="item in adminNav" :key="item.to" :to="item.to"
            :class="{ active: isActive(item.to) }" @click="mobileOpen = false"
          >
            <el-icon><component :is="item.icon" /></el-icon><span>{{ item.label }}</span>
          </RouterLink>
        </template>
      </nav>
      <div class="sidebar-note">
        <span>借阅规则</span>
        <p>借期 30 天，逾期状态按当前时间实时判断。</p>
      </div>
    </aside>
    <div v-if="mobileOpen" class="sidebar-scrim" @click="mobileOpen = false" />
    <section class="shell-main">
      <header class="topbar">
        <button class="mobile-menu" aria-label="打开导航" @click="mobileOpen = true">
          <el-icon><Fold /></el-icon>
        </button>
        <div><p>一方书室 / {{ auth.isAdmin && route.path.startsWith('/admin') ? '管理台' : '阅读台' }}</p><h1>{{ title }}</h1></div>
        <el-dropdown trigger="click">
          <button class="user-menu">
            <span class="user-monogram">{{ auth.currentUser?.username.slice(0, 1) }}</span>
            <span class="user-meta"><b>{{ auth.currentUser?.username }}</b><small>{{ auth.currentUser?.role }}</small></span>
            <el-icon><ArrowDown /></el-icon>
          </button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="router.push('/profile')">个人资料</el-dropdown-item>
              <el-dropdown-item divided @click="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </header>
      <main class="page-stage"><RouterView /></main>
    </section>
  </div>
</template>
