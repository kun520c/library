import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { authApi } from '@/api/auth'
import { TOKEN_KEY } from '@/api/client'
import type { User } from '@/types'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem(TOKEN_KEY))
  const currentUser = ref<User | null>(null)
  const initialized = ref(false)
  const isAdmin = computed(() => currentUser.value?.role === 'ADMIN')
  const isAuthenticated = computed(() => Boolean(token.value && currentUser.value))

  async function login(account: string, password: string) {
    const response = await authApi.login(account, password)
    token.value = response.accessToken
    localStorage.setItem(TOKEN_KEY, response.accessToken)
    currentUser.value = await authApi.me()
  }

  async function bootstrap() {
    if (initialized.value) return
    if (token.value) {
      try {
        currentUser.value = await authApi.me()
      } catch {
        token.value = null
        currentUser.value = null
        localStorage.removeItem(TOKEN_KEY)
      }
    }
    initialized.value = true
  }

  async function refreshUser() {
    currentUser.value = await authApi.me()
  }

  function logout() {
    token.value = null
    currentUser.value = null
    localStorage.removeItem(TOKEN_KEY)
  }

  return { token, currentUser, initialized, isAdmin, isAuthenticated, login, bootstrap, refreshUser, logout }
})
