import { request } from './client'
import type { Book, BookCreatePayload, BookQuery, BookUpdatePayload, PageVO } from '@/types'

export const booksApi = {
  list: (params: BookQuery) => request<PageVO<Book>>({ method: 'GET', url: '/book', params }),
  detail: (id: number) => request<Book>({ method: 'GET', url: `/book/${id}` }),
  create: (data: BookCreatePayload) => request<void>({ method: 'POST', url: '/book', data }),
  update: (id: number, data: BookUpdatePayload) =>
    request<void>({ method: 'PUT', url: `/book/${id}`, data }),
  adjustStock: (id: number, delta: number) =>
    request<void>({ method: 'PATCH', url: `/book/${id}/stock`, data: { delta } }),
  remove: (id: number) => request<void>({ method: 'DELETE', url: `/book/${id}` }),
}
