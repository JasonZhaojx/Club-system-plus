import { request } from '@/api/http'
import type { PageResult } from '@/api/types'

export interface Restaurant {
  id: number
  name: string
  address: string
  latitude: number
  longitude: number
  category: string
  priceLevel?: string | null
  websiteUrl?: string | null
  coverUrl?: string | null
  status: string
  ratingAvg: number
  reviewCount: number
  distanceMeters?: number | null
  recommendScore?: number | null
}

export interface RestaurantReview {
  id: number
  restaurantId: number
  userId: number
  username: string
  nickname: string
  avatarUrl?: string | null
  rating: number
  content?: string | null
  createdAt: string
  updatedAt: string
}

export interface NearbyRestaurantQuery {
  lat: number
  lng: number
  radius?: number
  category?: string
  page?: number
  size?: number
}

export interface RestaurantReviewPayload {
  rating: number
  content?: string | null
}

export function listNearbyRestaurants(query: NearbyRestaurantQuery) {
  return request<PageResult<Restaurant>>({
    method: 'GET',
    url: '/restaurants/nearby',
    params: query,
  })
}

export function getRestaurant(restaurantId: number) {
  return request<Restaurant>({
    method: 'GET',
    url: `/restaurants/${restaurantId}`,
  })
}

export function listRestaurantReviews(restaurantId: number, page = 1, size = 10) {
  return request<PageResult<RestaurantReview>>({
    method: 'GET',
    url: `/restaurants/${restaurantId}/reviews`,
    params: { page, size },
  })
}

export function saveRestaurantReview(restaurantId: number, payload: RestaurantReviewPayload) {
  return request<RestaurantReview>({
    method: 'POST',
    url: `/restaurants/${restaurantId}/reviews`,
    data: payload,
  })
}
