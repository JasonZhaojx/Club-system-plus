import { request } from '@/api/http'

export interface HealthInfo {
  status: string
  time: string
}

export function getHealth() {
  return request<HealthInfo>({
    url: '/health',
    method: 'GET',
  })
}
