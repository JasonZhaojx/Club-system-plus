import { useEffect, useMemo, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import {
  listMyActivityRegistrations,
  type ActivityRegistration,
} from '@/api/modules/activity'
import PageLoading from '@/components/PageLoading'
import { getErrorMessage } from '@/toast'

const fallbackImage =
  'https://images.unsplash.com/photo-1521737604893-d14cc237f11d?auto=format&fit=crop&w=1200&q=80'

const batchSize = 5

const registrationStatusNames: Record<ActivityRegistration['status'], string> = {
  REGISTERED: '已报名',
  CANCELLED: '已取消',
}

function formatDateTime(value: string) {
  return new Date(value).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

export default function MyActivitiesView() {
  const [registrations, setRegistrations] = useState<ActivityRegistration[]>([])
  const [visibleCount, setVisibleCount] = useState(batchSize)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const loadMoreRef = useRef<HTMLDivElement | null>(null)

  const activeRegistrations = useMemo(
    () => registrations.filter((registration) => registration.status === 'REGISTERED'),
    [registrations],
  )
  const visibleRegistrations = activeRegistrations.slice(0, visibleCount)
  const hasMore = visibleCount < activeRegistrations.length

  useEffect(() => {
    let active = true
    listMyActivityRegistrations()
      .then((records) => {
        if (active) {
          setRegistrations(records)
        }
      })
      .catch((err) => {
        if (active) {
          setError(getErrorMessage(err, '报名记录加载失败'))
        }
      })
      .finally(() => {
        if (active) {
          setLoading(false)
        }
      })
    return () => {
      active = false
    }
  }, [])

  useEffect(() => {
    const target = loadMoreRef.current
    if (!target || !hasMore) {
      return
    }
    const observer = new IntersectionObserver(
      ([entry]) => {
        if (entry.isIntersecting) {
          setVisibleCount((current) => Math.min(current + batchSize, activeRegistrations.length))
        }
      },
      { rootMargin: '220px 0px' },
    )
    observer.observe(target)
    return () => observer.disconnect()
  }, [activeRegistrations.length, hasMore])

  return (
    <section className="content public-page">
      <div className="section-head">
        <div>
          <p className="profile-eyebrow">My Activities</p>
          <h1>我的活动</h1>
          <p>查看当前账号报名过的活动、开始时间和报名状态。</p>
        </div>
        <Link className="back-button" to="/my">
          返回我的
        </Link>
      </div>

      {loading && <PageLoading className="inline-loading-section" />}
      {!loading && error && <div className="empty-state">{error}</div>}
      {!loading && !error && !activeRegistrations.length && (
        <div className="empty-state">暂无已报名活动</div>
      )}

      <div className="my-activity-list">
        {visibleRegistrations.map((registration) => (
          <Link
            className="my-activity-item"
            key={registration.id}
            to={`/activities/${registration.activityId}`}
          >
            <img
              alt={registration.activityTitle}
              src={registration.activityImageUrl || fallbackImage}
            />
            <div>
              <span className={`status-pill status-${registration.status.toLowerCase()}`}>
                {registrationStatusNames[registration.status]}
              </span>
              <h2>{registration.activityTitle}</h2>
              <p>{registration.activityLocation}</p>
              <p>{formatDateTime(registration.activityStartTime)}</p>
            </div>
          </Link>
        ))}
      </div>

      {!!activeRegistrations.length && (
        <div className="load-more-wrap" ref={loadMoreRef}>
          {hasMore ? <span>继续向下浏览，自动加载更多活动</span> : <span>已展示全部已报名活动</span>}
        </div>
      )}
    </section>
  )
}
