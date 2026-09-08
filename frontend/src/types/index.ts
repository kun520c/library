export type Role = 'USER' | 'ADMIN'
export type BorrowStatus = 'BORROWED' | 'RETURNED'

export interface ApiResult<T> {
  code: number
  message: string
  data: T
}

export interface PageVO<T> {
  list: T[]
  total: number
  page: number
  size: number
}

export interface User {
  id: number
  username: string
  account: string
  role: Role
}

export interface AdminUser extends User {
  createdAt: string
}

export interface LoginResponse {
  tokenType: 'Bearer'
  accessToken: string
  expiresIn: number
}

export interface Book {
  id: number
  title: string
  author: string
  isbn: string
  price: number
  stock: number
  categoryId: number | null
}

export interface Category {
  id: number
  name: string
  createdAt: string
  updatedAt: string
}

export interface BorrowRecord {
  recordId: number
  bookId: number
  bookTitle: string
  userId: number
  username: string
  borrowTime: string
  dueTime: string
  returnTime: string | null
  status: BorrowStatus
  overdue: boolean
}

export interface PaginationParams {
  page?: number
  size?: number
}

export interface BookQuery extends PaginationParams {
  title?: string
  author?: string
  isbn?: string
  categoryId?: number
}

export interface BorrowQuery extends PaginationParams {
  status?: BorrowStatus
  bookTitle?: string
}

export interface UserQuery extends PaginationParams {
  username?: string
  account?: string
}

export interface BookCreatePayload {
  title: string
  author: string
  isbn: string
  price: number
  stock: number
  categoryId?: number
}

export type BookUpdatePayload = Omit<BookCreatePayload, 'stock'>
