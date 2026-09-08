import { request } from './client'
import type { LoginResponse, User } from '@/types'

export const authApi = {
  login: (account: string, password: string) =>
    request<LoginResponse>({ method: 'POST', url: '/user/login', data: { account, password } }),
  register: (username: string, account: string, password: string) =>
    request<void>({ method: 'POST', url: '/user/register', data: { username, account, password } }),
  me: () => request<User>({ method: 'GET', url: '/user/me' }),
  updateProfile: (username: string) =>
    request<User>({ method: 'PUT', url: '/user/me', data: { username } }),
}
