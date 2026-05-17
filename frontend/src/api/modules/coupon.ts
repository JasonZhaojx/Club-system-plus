import { request } from '@/api/http'
import type { PageResult } from '@/api/types'

export type CouponBatchStatus = 'DRAFT' | 'ACTIVE' | 'DISABLED'
export type UserCouponStatus = 'UNUSED' | 'USED' | 'EXPIRED'

export interface CouponBatch {
  id: number
  name: string
  description?: string | null
  couponType: string
  benefitText: string
  stock: number
  claimedCount: number
  remainingCount: number
  claimStartTime: string
  claimEndTime: string
  expireTime: string
  allowedRoleCodes: string[]
  status: CouponBatchStatus
}

export interface CouponBatchPayload {
  name: string
  description?: string | null
  couponType: string
  benefitText: string
  stock: number
  claimStartTime: string
  claimEndTime: string
  expireTime: string
  allowedRoleCodes: string[]
}

export interface UserCoupon {
  id: number
  batchId: number
  batchName: string
  description?: string | null
  couponType: string
  benefitText: string
  status: UserCouponStatus
  claimedAt: string
  usedAt?: string | null
  expireTime: string
}

export interface CouponRedemption {
  id: number
  userCouponId: number
  batchId: number
  batchName: string
  scene?: string | null
  note?: string | null
  redeemedAt: string
}

export function listCouponBatches(query: { page?: number; size?: number; keyword?: string } = {}) {
  return request<PageResult<CouponBatch>>({
    method: 'GET',
    url: '/coupons/batches',
    params: query,
  })
}

export function listManageCouponBatches(
  query: { page?: number; size?: number; keyword?: string; status?: CouponBatchStatus } = {},
) {
  return request<PageResult<CouponBatch>>({
    method: 'GET',
    url: '/coupons/batches/manage',
    params: query,
  })
}

export function createCouponBatch(payload: CouponBatchPayload) {
  return request<CouponBatch>({
    method: 'POST',
    url: '/coupons/batches',
    data: payload,
  })
}

export function updateCouponBatch(batchId: number, payload: CouponBatchPayload) {
  return request<CouponBatch>({
    method: 'PUT',
    url: `/coupons/batches/${batchId}`,
    data: payload,
  })
}

export function claimCoupon(batchId: number) {
  return request<UserCoupon>({
    method: 'POST',
    url: `/coupons/batches/${batchId}/claim`,
  })
}

export function listMyCoupons() {
  return request<UserCoupon[]>({
    method: 'GET',
    url: '/coupons/me',
  })
}

export function useCoupon(userCouponId: number, payload: { scene?: string; note?: string } = {}) {
  return request<UserCoupon>({
    method: 'PATCH',
    url: `/coupons/me/${userCouponId}/use`,
    data: payload,
  })
}

export function listMyCouponRedemptions() {
  return request<CouponRedemption[]>({
    method: 'GET',
    url: '/coupons/redemptions/me',
  })
}
