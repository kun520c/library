import { request } from './client'
import type { Category } from '@/types'

export const categoriesApi = {
  list: () => request<Category[]>({ method: 'GET', url: '/category' }),
  detail: (id: number) => request<Category>({ method: 'GET', url: `/category/${id}` }),
  create: (name: string) => request<void>({ method: 'POST', url: '/category', data: { name } }),
  update: (id: number, name: string) =>
    request<void>({ method: 'PUT', url: `/category/${id}`, data: { name } }),
  remove: (id: number) => request<void>({ method: 'DELETE', url: `/category/${id}` }),
}
