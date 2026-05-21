import axios, {
  type AxiosError,
  type AxiosRequestConfig,
  type InternalAxiosRequestConfig,
} from 'axios'
import type { ApiResult } from './types'
import { clearAuth, getAccessToken } from '@/auth'
import { showToast } from '@/toast'

export interface AppRequestConfig extends AxiosRequestConfig {
  suppressGlobalError?: boolean
}

const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? '/api',
  timeout: 15000,
})

function defaultMessageByStatus(status?: number) {
  switch (status) {
    case 400:
      return '请求参数错误'
    case 401:
      return '未登录或登录已过期'
    case 403:
      return '无权限访问'
    case 404:
      return '资源不存在'
    case 409:
      return '业务冲突'
    case 429:
      return '请求过于频繁，请稍后再试'
    case 500:
      return '系统异常'
    default:
      return '网络异常，请稍后重试'
  }
}

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
      const message = result.message || '请求失败'
      if (!(response.config as AppRequestConfig).suppressGlobalError) {
        showToast(message)
      }
      return Promise.reject(new Error(message))
    }
    return response
  },
  (error: AxiosError<ApiResult<unknown>>) => {
    const config = error.config as AppRequestConfig | undefined
    const message =
      error.response?.data?.message ||
      defaultMessageByStatus(error.response?.status)
    if (error.response?.status === 401) {
      clearAuth()
    }
    if (!config?.suppressGlobalError) {
      showToast(message)
    }
    return Promise.reject(new Error(message))
  },
)

export async function request<T>(config: AppRequestConfig): Promise<T> {
  const response = await http.request<ApiResult<T>>(config)
  return response.data.data
}

export default http
