import { useEffect, useMemo, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { listActivities, updateActivityReview, type Activity } from '@/api/modules/activity'
import { uploadImage } from '@/api/modules/file'
import { getStoredUser, hasPermission } from '@/auth'
import { getErrorMessage, showToast } from '@/toast'

const batchSize = 10
const fallbackImage =
  'https://images.unsplash.com/photo-1521737604893-d14cc237f11d?auto=format&fit=crop&w=1200&q=80'

const categories = [
  { value: '', label: '全部活动' },
  { value: 'social', label: '娱乐·交友' },
  { value: 'welfare', label: '关怀·支持' },
  { value: 'sports', label: '体育·社交' },
  { value: 'culture', label: '文化活动' },
  { value: 'career', label: '职业·学术' },
  { value: 'major', label: '大型活动' },
]

type ActivityTab = 'open' | 'ended'

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

function isRegistrationOpen(activity: Activity) {
  return activity.status === 'PUBLISHED' && Date.now() < new Date(activity.startTime).getTime()
}

function LoadingLine() {
  return (
    <div className="loading-line" aria-label="正在加载">
      <span />
    </div>
  )
}

export default function ActivitiesView() {
  const currentUser = getStoredUser()
  const canEditReview = hasPermission(currentUser, 'activity:review')
  const [activities, setActivities] = useState<Activity[]>([])
  const [activeTab, setActiveTab] = useState<ActivityTab>('open')
  const [category, setCategory] = useState('')
  const [keyword, setKeyword] = useState('')
  const [sortBy, setSortBy] = useState('latest')
  const [page, setPage] = useState(1)
  const [total, setTotal] = useState(0)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [reviewImages, setReviewImages] = useState<Record<number, string>>({})
  const [reviewCopy, setReviewCopy] = useState<Record<number, string>>({})
  const [savingReviewIds, setSavingReviewIds] = useState<Record<number, boolean>>({})
  const loadMoreRef = useRef<HTMLDivElement | null>(null)

  const visibleActivities = useMemo(() => {
    return activities.filter((activity) =>
      activeTab === 'open' ? isRegistrationOpen(activity) : isActivityEnded(activity),
    )
  }, [activeTab, activities])

  const hasMore = activities.length < total

  useEffect(() => {
    void loadActivities(1, true)
  }, [activeTab, category, keyword, sortBy])

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
        status: activeTab === 'open' ? 'PUBLISHED' : 'ENDED',
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

  async function handleReviewImage(activity: Activity, file?: File) {
    if (!file) {
      return
    }
    setSavingReviewIds((current) => ({ ...current, [activity.id]: true }))
    try {
      const uploaded = await uploadImage(file, 'activity-review')
      const updated = await updateActivityReview(activity.id, {
        reviewImageUrl: uploaded.url,
        reviewContent: reviewCopy[activity.id] ?? activity.reviewContent ?? activity.summary,
      })
      setReviewImages((current) => ({ ...current, [activity.id]: uploaded.url }))
      setActivities((current) => current.map((item) => (item.id === updated.id ? updated : item)))
      showToast('活动照片已保存', 'success')
    } catch (err) {
      showToast(getErrorMessage(err, '活动照片保存失败'))
    } finally {
      setSavingReviewIds((current) => ({ ...current, [activity.id]: false }))
    }
  }

  async function saveReview(activity: Activity) {
    setSavingReviewIds((current) => ({ ...current, [activity.id]: true }))
    try {
      const updated = await updateActivityReview(activity.id, {
        reviewImageUrl: reviewImages[activity.id] ?? activity.reviewImageUrl ?? null,
        reviewContent: reviewCopy[activity.id] ?? activity.reviewContent ?? activity.summary,
      })
      setActivities((current) => current.map((item) => (item.id === updated.id ? updated : item)))
      showToast('活动回顾已保存', 'success')
    } catch (err) {
      showToast(getErrorMessage(err, '活动回顾保存失败'))
    } finally {
      setSavingReviewIds((current) => ({ ...current, [activity.id]: false }))
    }
  }

  return (
    <section className="content public-page">
      <div className="section-head">
        <div>
          <p className="profile-eyebrow">Activities</p>
          <h1>学联活动</h1>
          <p>从正在报名的校园活动，到已经结束的大型活动回顾，每一个时刻我们都陪伴在你身边。</p>
        </div>
        <Link className="secondary-link" to="/join">
          加入 / 联系学联
        </Link>
      </div>

      <div className="filters-shell activity-top-options">
        <div className="filters-panel">
          <div className="activity-status-tabs" aria-label="活动状态">
            <button
              className={activeTab === 'open' ? 'active' : ''}
              onClick={() => setActiveTab('open')}
              type="button"
            >
              正在报名的活动
            </button>
            <button
              className={activeTab === 'ended' ? 'active' : ''}
              onClick={() => setActiveTab('ended')}
              type="button"
            >
              已经结束的活动
            </button>
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
              <option value="capacity">名额优先</option>
            </select>
          </div>
        </div>
      </div>

      {loading && <LoadingLine />}

      <div className="activity-grid">
        {visibleActivities.map((activity) => {
          if (activeTab === 'ended') {
            const image = reviewImages[activity.id] || activity.reviewImageUrl || activity.imageUrl || fallbackImage
            const reviewText = reviewCopy[activity.id] ?? activity.reviewContent ?? activity.summary
            const saving = Boolean(savingReviewIds[activity.id])
            return (
              <article className="activity-card activity-card-ended activity-review-card" key={activity.id}>
                {canEditReview ? (
                  <label className="activity-review-upload">
                    <img alt={activity.title} src={image} />
                    <span>{reviewImages[activity.id] ? '更换活动照片' : '上传活动照片'}</span>
                    <input
                      accept="image/*"
                      disabled={saving}
                      onChange={(event) => handleReviewImage(activity, event.target.files?.[0])}
                      type="file"
                    />
                  </label>
                ) : (
                  <img alt={activity.title} src={image} />
                )}
                <div>
                  <div className="activity-card-kicker">
                    <span>{formatDate(activity.startTime)} · {activity.categoryName}</span>
                    <strong>已结束</strong>
                  </div>
                  <h3>{activity.title}</h3>
                  {canEditReview ? (
                    <textarea
                      aria-label={`${activity.title} 活动文案`}
                      onChange={(event) =>
                        setReviewCopy((current) => ({
                          ...current,
                          [activity.id]: event.target.value,
                        }))
                      }
                      placeholder="填写活动照片说明、活动回顾文案或精彩瞬间记录"
                      value={reviewText}
                    />
                  ) : (
                    <p>{reviewText}</p>
                  )}
                  <div className="activity-card-meta">
                    <strong>活动回顾</strong>
                    {canEditReview && (
                      <button disabled={saving} onClick={() => saveReview(activity)} type="button">
                        {saving ? '保存中' : '保存回顾'}
                      </button>
                    )}
                    <Link to={`/activities/${activity.id}`}>查看详情</Link>
                  </div>
                </div>
              </article>
            )
          }

          return (
            <Link className="activity-card" key={activity.id} to={`/activities/${activity.id}`}>
              <img alt={activity.title} src={activity.imageUrl || fallbackImage} />
              <div>
                <div className="activity-card-kicker">
                  <span>{formatDate(activity.startTime)} · {activity.categoryName}</span>
                  <strong className="activity-open-pill">报名中</strong>
                </div>
                <h3>{activity.title}</h3>
                <p>{activity.summary}</p>
                <div className="activity-card-meta">
                  <strong>{activity.registeredCount}/{activity.capacity}</strong>
                  <span>{activity.location}</span>
                </div>
              </div>
            </Link>
          )
        })}
      </div>

      {!visibleActivities.length && !loading && (
        <div className="empty-state">{error || '没有找到匹配的活动'}</div>
      )}

      <div className="load-more-wrap" ref={loadMoreRef}>
        {loading ? <span className="load-more-progress" aria-label="正在加载"><i /></span> : hasMore ? <span>继续向下浏览，自动加载更多活动</span> : <span>已展示全部活动</span>}
      </div>
    </section>
  )
}
