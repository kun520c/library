import { request } from './client'
import type { AdminUser, BorrowQuery, BorrowRecord, PageVO, UserQuery } from '@/types'

export const adminApi = {
  users: (params: UserQuery) => request<PageVO<AdminUser>>({ method: 'GET', url: '/admin/users', params }),
  borrows: (params: BorrowQuery) =>
    request<PageVO<BorrowRecord>>({ method: 'GET', url: '/admin/borrow', params }),
}
