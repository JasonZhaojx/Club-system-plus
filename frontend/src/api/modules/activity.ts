import { request } from '@/api/http'
import type { PageResult } from '@/api/types'

export type ActivityStatus = 'DRAFT' | 'PENDING_REVIEW' | 'PUBLISHED' | 'CANCELLED' | 'ENDED'
export type ActivityRegistrationStatus = 'REGISTERED' | 'CANCELLED'

export interface Activity {
  id: number
  title: string
  summary: string
  detail: string
  category: string
  categoryName: string
  imageUrl?: string | null
  location: string
  startTime: string
  endTime: string
  capacity: number
  registeredCount: number
  status: ActivityStatus
  requiredRoleCode?: string | null
  creatorId?: number | null
  publishedAt?: string | null
}

export interface ActivityPayload {
  title: string
  summary: string
  detail: string
  category: string
  categoryName: string
  imageUrl?: string | null
  location: string
  startTime: string
  endTime: string
  capacity: number
  requiredRoleCode?: string | null
}

export interface ActivityRegistration {
  id: number
  activityId: number
  activityTitle: string
  activityImageUrl?: string | null
  activityLocation: string
  activityStartTime: string
  status: ActivityRegistrationStatus
  registeredAt: string
}

export interface ActivityQuery {
  page?: number
  size?: number
  keyword?: string
  category?: string
  status?: ActivityStatus
  sort?: string
}

export function listActivities(query: ActivityQuery = {}) {
  return request<PageResult<Activity>>({
    method: 'GET',
    url: '/activities',
    params: query,
  })
}

export function getActivity(activityId: number) {
  return request<Activity>({
    method: 'GET',
    url: `/activities/${activityId}`,
  })
}

export function listManageActivities(query: ActivityQuery = {}) {
  return request<PageResult<Activity>>({
    method: 'GET',
    url: '/activities/manage',
    params: query,
  })
}

export function createActivity(payload: ActivityPayload) {
  return request<Activity>({
    method: 'POST',
    url: '/activities',
    data: payload,
  })
}

export function updateActivity(activityId: number, payload: ActivityPayload) {
  return request<Activity>({
    method: 'PUT',
    url: `/activities/${activityId}`,
    data: payload,
  })
}

export function submitActivity(activityId: number) {
  return request<Activity>({
    method: 'PATCH',
    url: `/activities/${activityId}/submit`,
  })
}

export function publishActivity(activityId: number) {
  return request<Activity>({
    method: 'PATCH',
    url: `/activities/${activityId}/publish`,
  })
}

export function cancelActivity(activityId: number) {
  return request<Activity>({
    method: 'PATCH',
    url: `/activities/${activityId}/cancel`,
  })
}

export function finishActivity(activityId: number) {
  return request<Activity>({
    method: 'PATCH',
    url: `/activities/${activityId}/finish`,
  })
}

export function registerActivity(activityId: number) {
  return request<ActivityRegistration>({
    method: 'POST',
    url: `/activities/${activityId}/registrations`,
  })
}

export function cancelActivityRegistration(activityId: number) {
  return request<void>({
    method: 'DELETE',
    url: `/activities/${activityId}/registrations`,
  })
}

export function listMyActivityRegistrations() {
  return request<ActivityRegistration[]>({
    method: 'GET',
    url: '/activities/registrations/me',
  })
}
