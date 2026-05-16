import type { AuthToken, UserProfile } from '@/api/modules/auth'

const ACCESS_TOKEN_KEY = 'access_token'
const USER_KEY = 'current_user'

export function saveAuth(auth: AuthToken) {
  localStorage.setItem(ACCESS_TOKEN_KEY, auth.accessToken)
  saveUser(auth.user)
}

export function saveUser(user: UserProfile) {
  localStorage.setItem(USER_KEY, JSON.stringify(user))
  window.dispatchEvent(new Event('auth-changed'))
}

export function clearAuth() {
  localStorage.removeItem(ACCESS_TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
  window.dispatchEvent(new Event('auth-changed'))
}

export function getAccessToken() {
  return localStorage.getItem(ACCESS_TOKEN_KEY)
}

export function getStoredUser(): UserProfile | null {
  const raw = localStorage.getItem(USER_KEY)
  if (!raw) {
    return null
  }
  try {
    return JSON.parse(raw) as UserProfile
  } catch {
    clearAuth()
    return null
  }
}

export function canAccessAdmin(user: UserProfile | null) {
  if (!user) {
    return false
  }
  return user.id === 1 || user.roles.includes('ADMIN') || user.permissions.includes('dashboard:view')
}
