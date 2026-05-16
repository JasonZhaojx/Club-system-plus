import { request } from '@/api/http'

export type DepartmentStatus = 'ACTIVE' | 'DISABLED'
export type MemberStatus = 'ACTIVE' | 'DISABLED' | 'REMOVED'

export interface Department {
  id: number
  name: string
  description?: string | null
  status: DepartmentStatus
}

export interface ClubMember {
  userId: number
  username: string
  nickname: string
  departmentId: number
  departmentName: string
  joinedAt: string
  status: MemberStatus
  departmentLeader: boolean
}

export interface DepartmentLeader {
  userId: number
  username: string
  nickname: string
  departmentId: number
  departmentName: string
  appointedAt: string
}

export interface PageResult<T> {
  records: T[]
  total: number
  page: number
  size: number
}

export interface AdminUser {
  id: number
  username: string
  nickname: string
  email?: string | null
  status: 'NORMAL' | 'DISABLED' | 'DELETED'
  departmentId?: number | null
  departmentName?: string | null
  joinedAt?: string | null
  memberStatus?: MemberStatus | null
  departmentLeader: boolean
  roles?: string | null
  createdAt: string
}

export function listDepartments() {
  return request<Department[]>({
    url: '/organization/departments',
    method: 'GET',
  })
}

export function createDepartment(payload: { name: string; description?: string | null }) {
  return request<Department>({
    url: '/organization/departments',
    method: 'POST',
    data: payload,
  })
}

export function updateDepartment(
  departmentId: number,
  payload: { name: string; description?: string | null },
) {
  return request<Department>({
    url: `/organization/departments/${departmentId}`,
    method: 'PUT',
    data: payload,
  })
}

export function disableDepartment(departmentId: number) {
  return request<void>({
    url: `/organization/departments/${departmentId}/disable`,
    method: 'PATCH',
  })
}

export function enableDepartment(departmentId: number) {
  return request<void>({
    url: `/organization/departments/${departmentId}/enable`,
    method: 'PATCH',
  })
}

export function listMembers(departmentId?: number) {
  return request<ClubMember[]>({
    url: '/organization/members',
    method: 'GET',
    params: { departmentId },
  })
}

export function listUsers(page = 1, size = 10, keyword?: string, departmentId?: number) {
  return request<PageResult<AdminUser>>({
    url: '/organization/users',
    method: 'GET',
    params: { page, size, keyword, departmentId },
  })
}

export function assignMemberToDepartment(payload: { userId: number; departmentId: number }) {
  return request<ClubMember>({
    url: '/organization/members',
    method: 'POST',
    data: payload,
  })
}

export function updateMemberStatus(payload: { userId: number; status: MemberStatus }) {
  return request<ClubMember>({
    url: '/organization/members/status',
    method: 'PATCH',
    data: payload,
  })
}

export function listDepartmentLeaders(departmentId?: number) {
  return request<DepartmentLeader[]>({
    url: '/organization/leaders',
    method: 'GET',
    params: { departmentId },
  })
}

export function appointDepartmentLeader(payload: { userId: number; departmentId: number }) {
  return request<void>({
    url: '/organization/leaders',
    method: 'POST',
    data: payload,
  })
}

export function removeDepartmentLeader(payload: { userId: number; departmentId: number }) {
  return request<void>({
    url: '/organization/leaders',
    method: 'DELETE',
    data: payload,
  })
}
