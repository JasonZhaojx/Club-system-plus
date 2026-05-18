import { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { listActivities, type Activity } from '@/api/modules/activity'
import { getErrorMessage } from '@/toast'

const batchSize = 10
const fallbackImage =
  'https://images.unsplash.com/photo-1521737604893-d14cc237f11d?auto=format&fit=crop&w=1200&q=80'

const categories = [
  { value: '', label: '全部活动' },
  { value: 'technology', label: '技术工作坊' },
  { value: 'career', label: '职业发展' },
  { value: 'community', label: '社群活动' },
  { value: 'competition', label: '挑战赛' },
]

function formatDate(value: string) {
  return new Date(value).toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

function isActivityEnded(activity: Activity) {
  return activity.status === 'ENDED' || Date.now() >= new Date(activity.endTime).getTime()
}

export default function ActivitiesView() {
  const [activities, setActivities] = useState<Activity[]>([])
  const [category, setCategory] = useState('')
  const [keyword, setKeyword] = useState('')
  const [sortBy, setSortBy] = useState('latest')
  const [page, setPage] = useState(1)
  const [total, setTotal] = useState(0)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const loadMoreRef = useRef<HTMLDivElement | null>(null)

  const hasMore = activities.length < total

  useEffect(() => {
    void loadActivities(1, true)
  }, [category, keyword, sortBy])

  useEffect(() => {
    const target = loadMoreRef.current
    if (!target || !hasMore || loading) {
      return
    }

    const observer = new IntersectionObserver(
      ([entry]) => {
        if (entry.isIntersecting) {
          void loadActivities(page + 1, false)
        }
      },
      { rootMargin: '260px 0px' },
    )

    observer.observe(target)
    return () => observer.disconnect()
  }, [hasMore, loading, page])

  async function loadActivities(nextPage: number, replace: boolean) {
    setLoading(true)
    setError('')
    try {
      const result = await listActivities({
        page: nextPage,
        size: batchSize,
        keyword: keyword.trim() || undefined,
        category: category || undefined,
        sort: sortBy,
      })
      setActivities((current) => (replace ? result.records : [...current, ...result.records]))
      setTotal(result.total)
      setPage(result.page)
    } catch (err) {
      setError(getErrorMessage(err, '活动加载失败'))
    } finally {
      setLoading(false)
    }
  }

  return (
    <section className="content public-page">
      <div className="section-head">
        <div>
          <p className="profile-eyebrow">Activities</p>
          <h1>活动列表</h1>
          <p>公开活动、工作坊、职业分享和社群活动都会在这里展示。</p>
        </div>
      </div>

      <div className="filter-bar">
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

      <div className="activity-tools">
        <input
          onChange={(event) => setKeyword(event.target.value)}
          placeholder="搜索活动标题、地点或关键词"
          value={keyword}
        />
        <select onChange={(event) => setSortBy(event.target.value)} value={sortBy}>
          <option value="latest">最新发布</option>
          <option value="upcoming">最近开始</option>
          <option value="capacity">剩余名额优先</option>
        </select>
      </div>

      <div className="activity-grid">
        {activities.map((activity) => {
          const ended = isActivityEnded(activity)
          return (
            <Link className={`activity-card${ended ? ' activity-card-ended' : ''}`} key={activity.id} to={`/activities/${activity.id}`}>
              <img alt={activity.title} src={activity.imageUrl || fallbackImage} />
              <div>
                <div className="activity-card-kicker">
                  <span>{formatDate(activity.startTime)} · {activity.categoryName}</span>
                  {ended && <strong>已结束</strong>}
                </div>
                <h3>{activity.title}</h3>
                <p>{activity.summary}</p>
                <div className="activity-card-meta">
                  <strong>{ended ? '活动已结束' : `${activity.registeredCount}/${activity.capacity}`}</strong>
                  <span>{activity.location}</span>
                </div>
              </div>
            </Link>
          )
        })}
      </div>

      {!activities.length && !loading && (
        <div className="empty-state">{error || '没有找到匹配的活动'}</div>
      )}

      <div className="load-more-wrap" ref={loadMoreRef}>
        {loading ? <span>正在加载活动...</span> : hasMore ? <span>继续向下浏览，自动加载更多活动</span> : <span>已展示全部活动</span>}
      </div>
    </section>
  )
}
