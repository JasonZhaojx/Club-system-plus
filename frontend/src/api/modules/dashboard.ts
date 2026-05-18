import { request } from '@/api/http'

export interface DashboardMetric {
  label: string
  value: number
  tone: string
}

export interface DashboardNameValue {
  name: string
  value: number
}

export interface DashboardRank {
  name: string
  value: number
  detail: string | null
  rate: number | null
}

export interface DashboardConversion {
  name: string
  current: number
  target: number
  rate: number
}

export interface ApiTrafficPoint {
  bucket: string
  total: number
  errorCount: number
  avgDurationMs: number | null
}

export interface ApiAccessLog {
  id: number
  method: string
  path: string
  statusCode: number
  durationMs: number
  userId: number | null
  username: string | null
  ipAddress: string | null
  createdAt: string
}

export interface OperationLog {
  id: number
  userId: number | null
  username: string | null
  method: string
  path: string
  action: string
  statusCode: number
  durationMs: number
  ipAddress: string | null
  createdAt: string
}

export interface DashboardOverview {
  refreshedAt: string
  metrics: DashboardMetric[]
  activityStatus: DashboardNameValue[]
  memberStatus: DashboardNameValue[]
  apiTraffic: ApiTrafficPoint[]
  hotApis: DashboardRank[]
  slowApis: DashboardRank[]
  errorApis: DashboardRank[]
  activityConversions: DashboardConversion[]
  couponConversions: DashboardConversion[]
  activeUsers: DashboardRank[]
  apiLogs: ApiAccessLog[]
  operationLogs: OperationLog[]
}

export function getDashboardOverview() {
  return request<DashboardOverview>({
    url: '/dashboard/overview',
    method: 'GET',
  })
}
