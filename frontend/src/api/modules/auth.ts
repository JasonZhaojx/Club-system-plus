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
  membership?: {
    departmentId: number
    departmentName: string
    joinedAt: string
  } | null
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

export interface PasswordResetCodePayload {
  email: string
}

export interface PasswordResetConfirmPayload extends PasswordResetCodePayload {
  code: string
  newPassword: string
}

export function login(payload: LoginPayload) {
  return request<AuthToken>({
    url: '/auth/login',
    method: 'POST',
    data: payload,
    suppressGlobalError: true,
  })
}

export function register(payload: RegisterPayload) {
  return request<AuthToken>({
    url: '/auth/register',
    method: 'POST',
    data: payload,
    suppressGlobalError: true,
  })
}

export function sendPasswordResetCode(payload: PasswordResetCodePayload) {
  return request<void>({
    url: '/auth/password-reset/code',
    method: 'POST',
    data: payload,
    suppressGlobalError: true,
  })
}

export function confirmPasswordReset(payload: PasswordResetConfirmPayload) {
  return request<void>({
    url: '/auth/password-reset/confirm',
    method: 'POST',
    data: payload,
    suppressGlobalError: true,
  })
}

export function getCurrentUser() {
  return request<UserProfile>({
    url: '/auth/me',
    method: 'GET',
  })
}

export function getProfile() {
  return request<UserProfile>({
    url: '/users/me',
    method: 'GET',
  })
}

export function updateProfile(payload: { nickname: string; email?: string | null }) {
  return request<UserProfile>({
    url: '/users/me',
    method: 'PATCH',
    data: payload,
  })
}

export function changePassword(oldPassword: string, newPassword: string) {
  return request<void>({
    url: '/users/me/password',
    method: 'PUT',
    data: { oldPassword, newPassword },
    suppressGlobalError: true,
  })
}

export function logout() {
  return request<void>({
    url: '/auth/logout',
    method: 'POST',
  })
}
