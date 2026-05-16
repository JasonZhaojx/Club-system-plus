export interface ApiResult<T> {
  code: number
  message: string
  data: T
}

export interface PageQuery {
  page?: number
  size?: number
}

export interface PageResult<T> {
  records: T[]
  total: number
  page: number
  size: number
}
