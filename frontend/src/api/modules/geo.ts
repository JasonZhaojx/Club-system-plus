import { request } from '@/api/http'

export interface PlaceSuggestion {
  placeId: string
  mainText: string
  secondaryText: string
  fullText: string
}

export interface PlaceDetail {
  placeId: string
  displayName: string
  formattedAddress: string
  latitude: number
  longitude: number
}

export function autocompletePlaces(input: string, lat?: number, lng?: number, sessionToken?: string) {
  return request<PlaceSuggestion[]>({
    method: 'GET',
    url: '/geo/places/autocomplete',
    params: {
      input,
      lat,
      lng,
      sessionToken,
    },
    suppressGlobalError: true,
  })
}

export function getPlaceDetail(placeId: string, sessionToken?: string) {
  return request<PlaceDetail>({
    method: 'GET',
    url: `/geo/places/${encodeURIComponent(placeId)}`,
    params: {
      sessionToken,
    },
  })
}
