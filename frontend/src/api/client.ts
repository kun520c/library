import axios, { type AxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import type { ApiResult } from '@/types'

const TOKEN_KEY = 'library.token'

const client = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 12_000,
  headers: { 'Content-Type': 'application/json' },
})

client.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

client.interceptors.response.use(
  (response) => response,
  (error: unknown) => {
    if (axios.isAxiosError<ApiResult<null>>(error)) {
      const status = error.response?.status
      if (!error.response) {
        ElMessage.error('无法连接到服务器，请确认后端服务已启动')
      } else if (status === 401) {
        localStorage.removeItem(TOKEN_KEY)
        if (location.pathname.startsWith('/login')) {
          ElMessage.error(error.response.data?.message || '账号或密码错误')
        } else {
          const redirect = encodeURIComponent(location.pathname + location.search)
          location.assign(`/login?redirect=${redirect}`)
        }
      } else if (status === 403) {
        ElMessage.warning(error.response.data?.message || '当前账号没有此操作权限')
      } else if (status === 500) {
        ElMessage.error('系统暂时不可用，请稍后重试')
      } else {
        ElMessage.error(error.response.data?.message || '请求未能完成')
      }
    } else {
      ElMessage.error('请求未能完成')
    }
    return Promise.reject(error)
  },
)

export async function request<T>(config: AxiosRequestConfig): Promise<T> {
  const response = await client.request<ApiResult<T>>(config)
  return response.data.data
}

export { TOKEN_KEY }
