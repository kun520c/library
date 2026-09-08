import { request } from './client'
import type { BorrowQuery, BorrowRecord, PageVO } from '@/types'

export const borrowApi = {
  borrow: (bookId: number) => request<BorrowRecord>({ method: 'POST', url: `/borrow/${bookId}` }),
  returnBook: (recordId: number) => request<void>({ method: 'POST', url: `/borrow/${recordId}/return` }),
  detail: (recordId: number) => request<BorrowRecord>({ method: 'GET', url: `/borrow/${recordId}` }),
  mine: (params: BorrowQuery) => request<PageVO<BorrowRecord>>({ method: 'GET', url: '/borrow/my', params }),
}
