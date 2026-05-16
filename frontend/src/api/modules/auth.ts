import { request } from '@/api/http'

export type UserStatus = 'NORMAL' | 'DISABLED' | 'DELETED'

export interface UserProfile {
  id: number
  username: string
  nickname: string
  email?: string | null
  status: UserStatus
  roles: string[]
  permissions: string[]
}

export interface AuthToken {
  accessToken: string
  tokenType: 'Bearer'
  expiresIn: number
  user: UserProfile
}

export interface LoginPayload {
  username: string
  password: string
}

export interface RegisterPayload extends LoginPayload {
  nickname?: string
  email?: string
}

export function login(payload: LoginPayload) {
  return request<AuthToken>({
    url: '/auth/login',
    method: 'POST',
    data: payload,
  })
}

export function register(payload: RegisterPayload) {
  return request<AuthToken>({
    url: '/auth/register',
    method: 'POST',
    data: payload,
  })
}

export function getCurrentUser() {
  return request<UserProfile>({
    url: '/auth/me',
    method: 'GET',
  })
}

export function logout() {
  return request<void>({
    url: '/auth/logout',
    method: 'POST',
  })
}
