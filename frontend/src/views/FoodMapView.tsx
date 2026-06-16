import 'leaflet/dist/leaflet.css'

import L from 'leaflet'
import { useEffect, useMemo, useState } from 'react'
import { Circle, MapContainer, Marker, Popup, TileLayer, useMap, useMapEvents } from 'react-leaflet'
import { autocompletePlaces, getPlaceDetail, type PlaceSuggestion } from '@/api/modules/geo'
import {
  getRestaurant,
  listNearbyRestaurants,
  listRestaurantReviews,
  saveRestaurantReview,
  type Restaurant,
  type RestaurantReview,
} from '@/api/modules/restaurant'
import { getAccessToken } from '@/auth'
import { getErrorMessage, showToast } from '@/toast'

const UNSW_LOCATION = { lat: -33.9173, lng: 151.2313 }
const defaultRadius = 1500
const reviewPageSize = 6

const categories = [
  { value: '', label: '全部' },
  { value: 'thai', label: '泰餐' },
  { value: 'malaysian', label: '马来西亚' },
  { value: 'indonesian', label: '印尼' },
  { value: 'japanese', label: '日料' },
  { value: 'italian', label: '意餐' },
  { value: 'dessert', label: '甜品' },
  { value: 'cafe', label: '咖啡' },
]

function markerIcon(active: boolean) {
  return L.divIcon({
    className: active ? 'food-marker food-marker-active' : 'food-marker',
    html: '<span></span>',
    iconSize: [26, 26],
    iconAnchor: [13, 13],
  })
}

function formatDistance(distance?: number | null) {
  if (distance == null) {
    return '距离未知'
  }
  return distance >= 1000 ? `${(distance / 1000).toFixed(1)} km` : `${distance} m`
}

function formatDate(value: string) {
  return new Date(value).toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

function googleMapsDirectionsUrl(restaurant: Restaurant, origin: { lat: number; lng: number }) {
  const start = `${origin.lat},${origin.lng}`
  const destination = `${restaurant.latitude},${restaurant.longitude}`
  return `https://www.google.com/maps/dir/?api=1&origin=${encodeURIComponent(start)}&destination=${encodeURIComponent(destination)}&travelmode=walking`
}

function MapRecenter({ lat, lng }: { lat: number; lng: number }) {
  const map = useMap()

  useEffect(() => {
    map.setView([lat, lng], map.getZoom(), { animate: true })
  }, [lat, lng, map])

  return null
}

function LocationPicker({ onPick }: { onPick: (lat: number, lng: number) => void }) {
  useMapEvents({
    click(event) {
      onPick(event.latlng.lat, event.latlng.lng)
    },
  })

  return null
}

export default function FoodMapView() {
  const [center, setCenter] = useState(UNSW_LOCATION)
  const [accuracyMeters, setAccuracyMeters] = useState<number | null>(null)
  const [radius, setRadius] = useState(defaultRadius)
  const [category, setCategory] = useState('')
  const [restaurants, setRestaurants] = useState<Restaurant[]>([])
  const [selected, setSelected] = useState<Restaurant | null>(null)
  const [reviews, setReviews] = useState<RestaurantReview[]>([])
  const [placeQuery, setPlaceQuery] = useState('')
  const [placeSuggestions, setPlaceSuggestions] = useState<PlaceSuggestion[]>([])
  const [placesLoading, setPlacesLoading] = useState(false)
  const [placeSessionToken, setPlaceSessionToken] = useState(() => crypto.randomUUID())
  const [reviewPage, setReviewPage] = useState(1)
  const [reviewTotal, setReviewTotal] = useState(0)
  const [rating, setRating] = useState(5)
  const [content, setContent] = useState('')
  const [loading, setLoading] = useState(false)
  const [reviewsLoading, setReviewsLoading] = useState(false)
  const [locationMessage, setLocationMessage] = useState('默认展示 UNSW Kensington 周边')
  const isLoggedIn = Boolean(getAccessToken())

  const selectedIcon = useMemo(() => markerIcon(true), [])
  const defaultIcon = useMemo(() => markerIcon(false), [])

  useEffect(() => {
    void loadNearby(center.lat, center.lng, radius, category)
  }, [center.lat, center.lng, radius, category])

  useEffect(() => {
    if (!selected) {
      return
    }
    void loadReviews(selected.id, 1, true)
  }, [selected?.id])

  useEffect(() => {
    const keyword = placeQuery.trim()
    if (keyword.length < 2) {
      setPlaceSuggestions([])
      return
    }
    const timer = window.setTimeout(() => {
      void loadPlaceSuggestions(keyword)
    }, 350)
    return () => window.clearTimeout(timer)
  }, [placeQuery, center.lat, center.lng, placeSessionToken])

  async function loadNearby(lat: number, lng: number, nextRadius: number, nextCategory: string) {
    setLoading(true)
    try {
      const result = await listNearbyRestaurants({
        lat,
        lng,
        radius: nextRadius,
        category: nextCategory || undefined,
        page: 1,
        size: 30,
      })
      setRestaurants(result.records)
      if (!selected || !result.records.some((item) => item.id === selected.id)) {
        setSelected(result.records[0] ?? null)
      }
    } catch (err) {
      showToast(getErrorMessage(err, '附近餐厅加载失败'))
    } finally {
      setLoading(false)
    }
  }

  async function selectRestaurant(restaurant: Restaurant) {
    setSelected(restaurant)
    try {
      const detail = await getRestaurant(restaurant.id)
      setSelected({ ...restaurant, ...detail })
    } catch (err) {
      showToast(getErrorMessage(err, '餐厅详情加载失败'))
    }
  }

  async function loadPlaceSuggestions(keyword: string) {
    setPlacesLoading(true)
    try {
      const result = await autocompletePlaces(keyword, center.lat, center.lng, placeSessionToken)
      setPlaceSuggestions(result)
    } catch {
      setPlaceSuggestions([])
    } finally {
      setPlacesLoading(false)
    }
  }

  async function selectPlace(suggestion: PlaceSuggestion) {
    try {
      const detail = await getPlaceDetail(suggestion.placeId, placeSessionToken)
      setCenter({ lat: Number(detail.latitude), lng: Number(detail.longitude) })
      setAccuracyMeters(null)
      setPlaceQuery(detail.formattedAddress || detail.displayName || suggestion.fullText)
      setPlaceSuggestions([])
      setPlaceSessionToken(crypto.randomUUID())
      setLocationMessage(`已使用 Google Places 确认位置：${detail.formattedAddress || detail.displayName}`)
    } catch (err) {
      showToast(getErrorMessage(err, '地点详情加载失败'))
    }
  }

  async function loadReviews(restaurantId: number, page: number, replace: boolean) {
    setReviewsLoading(true)
    try {
      const result = await listRestaurantReviews(restaurantId, page, reviewPageSize)
      setReviews((current) => (replace ? result.records : [...current, ...result.records]))
      setReviewPage(result.page)
      setReviewTotal(result.total)
    } catch (err) {
      showToast(getErrorMessage(err, '评论加载失败'))
    } finally {
      setReviewsLoading(false)
    }
  }

  async function useCurrentLocation() {
    if (!navigator.geolocation) {
      setLocationMessage('当前浏览器不支持定位，继续使用校园默认位置')
      return
    }
    navigator.geolocation.getCurrentPosition(
      (position) => {
        const accuracy = Math.round(position.coords.accuracy)
        setAccuracyMeters(accuracy)
        if (accuracy > 1000) {
          setLocationMessage(`浏览器定位精度约 ${accuracy} m，偏差较大；已保留 UNSW 默认位置，可点击地图手动设点`)
          return
        }
        setCenter({
          lat: position.coords.latitude,
          lng: position.coords.longitude,
        })
        setLocationMessage(`已根据你的当前位置推荐附近餐厅，定位精度约 ${accuracy} m`)
      },
      () => {
        setLocationMessage('定位未授权或失败，继续使用 UNSW 默认位置')
      },
      { enableHighAccuracy: true, timeout: 8000 },
    )
  }

  function useUnswLocation() {
    setCenter(UNSW_LOCATION)
    setAccuracyMeters(null)
    setLocationMessage('已切换到 UNSW Kensington 默认位置')
  }

  function pickMapLocation(lat: number, lng: number) {
    setCenter({ lat, lng })
    setAccuracyMeters(null)
    setLocationMessage('已使用你在地图上点击的位置作为推荐中心')
  }

  async function submitReview() {
    if (!selected) {
      return
    }
    try {
      await saveRestaurantReview(selected.id, {
        rating,
        content: content.trim() || null,
      })
      setContent('')
      showToast('评价已保存', 'success')
      await loadReviews(selected.id, 1, true)
      const detail = await getRestaurant(selected.id)
      setSelected((current) => (current ? { ...current, ...detail } : detail))
      setRestaurants((current) => current.map((item) => (item.id === detail.id ? { ...item, ...detail } : item)))
    } catch (err) {
      showToast(getErrorMessage(err, '评价保存失败'))
    }
  }

  const hasMoreReviews = reviews.length < reviewTotal

  return (
    <section className="content food-map-page">
      <div className="food-hero">
        <div>
          <p className="profile-eyebrow">Food Map</p>
          <h1>校园美食地图</h1>
          <p>根据你的位置推荐 UNSW 周边餐厅，评分和评论来自站内用户。</p>
        </div>
        <div className="food-hero-actions">
          <button onClick={useCurrentLocation} type="button">
            使用我的位置
          </button>
          <button onClick={useUnswLocation} type="button">
            UNSW 默认点
          </button>
        </div>
      </div>

      <div className="food-toolbar">
        <span>
          {locationMessage}
          {accuracyMeters != null && accuracyMeters <= 1000 ? ` · ${center.lat.toFixed(5)}, ${center.lng.toFixed(5)}` : ''}
        </span>
        <select onChange={(event) => setRadius(Number(event.target.value))} value={radius}>
          <option value={800}>800 m</option>
          <option value={1500}>1.5 km</option>
          <option value={3000}>3 km</option>
          <option value={5000}>5 km</option>
        </select>
        <div className="food-categories">
          {categories.map((item) => (
            <button
              className={category === item.value ? 'active' : ''}
              key={item.value}
              onClick={() => setCategory(item.value)}
              type="button"
            >
              {item.label}
            </button>
          ))}
        </div>
      </div>

      <div className="place-search-box">
        <label>
          <span>搜索并确认你的位置</span>
          <input
            onChange={(event) => setPlaceQuery(event.target.value)}
            placeholder="例如 UNSW Library, Kensington NSW"
            value={placeQuery}
          />
        </label>
        {(placeSuggestions.length > 0 || placesLoading) && (
          <div className="place-suggestions">
            {placesLoading && <p>正在查询 Google Places...</p>}
            {placeSuggestions.map((item) => (
              <button key={item.placeId} onClick={() => selectPlace(item)} type="button">
                <strong>{item.mainText}</strong>
                <span>{item.secondaryText || item.fullText}</span>
              </button>
            ))}
          </div>
        )}
      </div>

      <div className="food-map-layout">
        <div className="food-map-shell">
          <MapContainer center={[center.lat, center.lng]} className="food-map" scrollWheelZoom zoom={15}>
            <MapRecenter lat={center.lat} lng={center.lng} />
            <LocationPicker onPick={pickMapLocation} />
            <TileLayer
              attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
              url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
            />
            <Circle center={[center.lat, center.lng]} pathOptions={{ color: '#147d64', fillOpacity: 0.05 }} radius={radius} />
            {restaurants.map((restaurant) => (
              <Marker
                eventHandlers={{ click: () => selectRestaurant(restaurant) }}
                icon={selected?.id === restaurant.id ? selectedIcon : defaultIcon}
                key={restaurant.id}
                position={[Number(restaurant.latitude), Number(restaurant.longitude)]}
              >
                <Popup>
                  <strong>{restaurant.name}</strong>
                  <br />
                  {restaurant.ratingAvg?.toFixed(1)} / 5 · {formatDistance(restaurant.distanceMeters)}
                </Popup>
              </Marker>
            ))}
          </MapContainer>
          {loading && <div className="food-map-loading">正在加载附近餐厅...</div>}
          <div className="food-map-hint">点击地图任意位置，可手动设置推荐中心</div>
        </div>

        <aside className="food-side-panel">
          <div className="restaurant-list">
            {restaurants.map((restaurant) => (
              <button
                className={selected?.id === restaurant.id ? 'restaurant-row active' : 'restaurant-row'}
                key={restaurant.id}
                onClick={() => selectRestaurant(restaurant)}
                type="button"
              >
                <img alt={restaurant.name} src={restaurant.coverUrl || 'https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?auto=format&fit=crop&w=500&q=80'} />
                <span>
                  <strong>{restaurant.name}</strong>
                  <small>{restaurant.ratingAvg?.toFixed(1)} 分 · {restaurant.reviewCount} 条评价 · {formatDistance(restaurant.distanceMeters)}</small>
                </span>
              </button>
            ))}
            {!restaurants.length && !loading && <div className="empty-state">附近暂无餐厅数据</div>}
          </div>

          {selected && (
            <div className="restaurant-detail">
              <img alt={selected.name} src={selected.coverUrl || 'https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?auto=format&fit=crop&w=900&q=80'} />
              <div className="restaurant-detail-copy">
                <span className="restaurant-category">{selected.category}</span>
                <h2>{selected.name}</h2>
                <p>{selected.address}</p>
                <div className="restaurant-stats">
                  <strong>{selected.ratingAvg?.toFixed(1)} / 5</strong>
                  <span>{selected.reviewCount} 条评价</span>
                  <span>{formatDistance(selected.distanceMeters)}</span>
                </div>
                <div className="restaurant-links">
                  {selected.websiteUrl && (
                    <a href={selected.websiteUrl} rel="noreferrer" target="_blank">
                      查看餐厅网站
                    </a>
                  )}
                  <a href={googleMapsDirectionsUrl(selected, center)} rel="noreferrer" target="_blank">
                    Google Maps 导航
                  </a>
                </div>
              </div>

              <div className="review-editor">
                <h3>发表你的评价</h3>
                {isLoggedIn ? (
                  <>
                    <select onChange={(event) => setRating(Number(event.target.value))} value={rating}>
                      {[5, 4, 3, 2, 1].map((value) => (
                        <option key={value} value={value}>{value} 星</option>
                      ))}
                    </select>
                    <textarea
                      onChange={(event) => setContent(event.target.value)}
                      placeholder="写下口味、环境、价格或活动后聚餐体验"
                      value={content}
                    />
                    <button onClick={submitReview} type="button">保存评价</button>
                  </>
                ) : (
                  <p>登录后可以评分和评论。</p>
                )}
              </div>

              <div className="reviews-panel">
                <h3>用户评论</h3>
                {reviews.map((review) => (
                  <article className="review-item" key={review.id}>
                    <div>
                      <strong>{review.nickname || review.username}</strong>
                      <span>{review.rating} 星 · {formatDate(review.updatedAt)}</span>
                    </div>
                    <p>{review.content || '这个用户只留下了评分。'}</p>
                  </article>
                ))}
                {!reviews.length && !reviewsLoading && <p className="muted-line">暂无评论，成为第一个评价的人。</p>}
                {hasMoreReviews && (
                  <button disabled={reviewsLoading} onClick={() => selected && loadReviews(selected.id, reviewPage + 1, false)} type="button">
                    {reviewsLoading ? '加载中' : '加载更多评论'}
                  </button>
                )}
              </div>
            </div>
          )}
        </aside>
      </div>
    </section>
  )
}
