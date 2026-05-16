import axios, {
  type AxiosError,
  type AxiosRequestConfig,
  type InternalAxiosRequestConfig,
} from 'axios'
import type { ApiResult } from './types'
import { clearAuth, getAccessToken } from '@/auth'

const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? '/api',
  timeout: 15000,
})

http.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = getAccessToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  (response) => {
    const result = response.data as ApiResult<unknown>
    if (typeof result?.code === 'number' && result.code !== 0) {
      return Promise.reject(new Error(result.message || 'Request failed'))
    }
    return response
  },
  (error: AxiosError<ApiResult<unknown>>) => {
    if (error.response?.status === 401) {
      clearAuth()
    }
    return Promise.reject(error)
  },
)

export async function request<T>(config: AxiosRequestConfig): Promise<T> {
  const response = await http.request<ApiResult<T>>(config)
  return response.data.data
}

export default http
