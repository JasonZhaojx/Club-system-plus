import { request } from '@/api/http'

export interface Role {
  id: number
  code: string
  name: string
  description?: string | null
}

export interface Permission {
  id: number
  code: string
  name: string
  description?: string | null
}

export function listRoles() {
  return request<Role[]>({
    url: '/rbac/roles',
    method: 'GET',
  })
}

export function listPermissions() {
  return request<Permission[]>({
    url: '/rbac/permissions',
    method: 'GET',
  })
}

export function assignUserRoles(payload: { userId: number; roleCodes: string[] }) {
  return request<void>({
    url: '/rbac/users/roles',
    method: 'POST',
    data: payload,
  })
}
