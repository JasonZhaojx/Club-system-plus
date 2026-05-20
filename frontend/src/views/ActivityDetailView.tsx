import { useEffect, useMemo, useState } from 'react'
import { Navigate, useNavigate, useParams } from 'react-router-dom'
import {
  cancelActivityRegistration,
  getActivity,
  listMyActivityRegistrations,
  registerActivity,
  type Activity,
} from '@/api/modules/activity'
import { getAccessToken, getStoredUser } from '@/auth'
import PageLoading from '@/components/PageLoading'
import { getErrorMessage, showToast } from '@/toast'

const fallbackImage =
  'https://images.unsplash.com/photo-1521737604893-d14cc237f11d?auto=format&fit=crop&w=1200&q=80'

function formatDateTime(value: string) {
  return new Date(value).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

function isActivityEnded(activity: Activity) {
  return activity.status === 'ENDED' || Date.now() >= new Date(activity.endTime).getTime()
}

export default function ActivityDetailView() {
  const { activityId } = useParams()
  const navigate = useNavigate()
  const numericActivityId = Number(activityId)
  const user = getStoredUser()
  const [activity, setActivity] = useState<Activity | null>(null)
  const [registered, setRegistered] = useState(false)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [notFound, setNotFound] = useState(false)

  const remaining = activity ? Math.max(activity.capacity - activity.registeredCount, 0) : 0
  const registrationClosed = activity ? Date.now() >= new Date(activity.startTime).getTime() : false
  const roleAllowed = useMemo(() => {
    if (!activity?.requiredRoleCode) {
      return true
    }
    return roleSatisfies(user?.roles || [], activity.requiredRoleCode)
  }, [activity?.requiredRoleCode, user?.roles])

  useEffect(() => {
    if (!Number.isFinite(numericActivityId)) {
      setNotFound(true)
      return
    }
    let active = true
    Promise.all([
      getActivity(numericActivityId),
      getAccessToken() ? listMyActivityRegistrations() : Promise.resolve([]),
    ])
      .then(([detail, registrations]) => {
        if (!active) {
          return
        }
        setActivity(detail)
        setRegistered(registrations.some((item) => item.activityId === detail.id && item.status === 'REGISTERED'))
      })
      .catch(() => {
        if (active) {
          setNotFound(true)
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
  }, [numericActivityId])

  async function handleRegister() {
    if (!activity) {
      return
    }
    if (!getAccessToken()) {
      navigate(`/login?redirect=/activities/${activity.id}`)
      return
    }
    setSaving(true)
    try {
      await registerActivity(activity.id)
      setRegistered(true)
      setActivity({ ...activity, registeredCount: activity.registeredCount + 1 })
      showToast('报名成功', 'success')
    } catch (err) {
      showToast(getErrorMessage(err, '报名失败'))
    } finally {
      setSaving(false)
    }
  }

  async function handleCancelRegistration() {
    if (!activity) {
      return
    }
    setSaving(true)
    try {
      await cancelActivityRegistration(activity.id)
      setRegistered(false)
      setActivity({ ...activity, registeredCount: Math.max(activity.registeredCount - 1, 0) })
      showToast('已取消报名', 'success')
    } catch (err) {
      showToast(getErrorMessage(err, '取消报名失败'))
    } finally {
      setSaving(false)
    }
  }

  if (notFound) {
    return <Navigate to="/activities" replace />
  }

  if (loading || !activity) {
    return <PageLoading className="content public-page" />
  }

  const ended = isActivityEnded(activity)

  return (
    <section className="content public-page">
      <article className={`detail-article activity-detail-layout${ended ? ' activity-detail-ended' : ''}`}>
        <img alt={activity.title} src={activity.imageUrl || fallbackImage} />
        <div className="detail-body">
          <button className="back-button" onClick={() => navigate(-1)} type="button">
            返回
          </button>
          <div className="detail-kicker-row">
            <p className="profile-eyebrow">{activity.categoryName}</p>
            {ended && <span className="activity-ended-pill">已结束</span>}
          </div>
          <h1>{activity.title}</h1>
          <div className="detail-meta">
            <span>{formatDateTime(activity.startTime)}</span>
            <span>{activity.location}</span>
            {!ended && <span>{remaining} / {activity.capacity} 个名额可用</span>}
          </div>
          <p>{activity.detail}</p>
        </div>
        {!ended && (
          <aside className="activity-apply-panel">
            <span>报名状态</span>
            <strong>{registered ? '已报名' : remaining > 0 ? '开放报名' : '名额已满'}</strong>
            <p>{roleAllowed ? '你可以报名参加该活动。' : '当前账号暂不可报名该活动。'}</p>
            <div className="activity-capacity-bar">
              <span style={{ width: `${Math.min((activity.registeredCount / activity.capacity) * 100, 100)}%` }} />
            </div>
            {registered ? (
              <button className="cancel-registration-button" disabled={saving} onClick={handleCancelRegistration} type="button">
                取消报名
              </button>
            ) : (
              <button disabled={saving || remaining <= 0 || !roleAllowed || registrationClosed} onClick={handleRegister} type="button">
                {registrationClosed ? '报名已截止' : roleAllowed ? '立即报名' : '当前身份不可报名'}
              </button>
            )}
          </aside>
        )}
      </article>
    </section>
  )
}

function roleSatisfies(roles: string[], requiredRoleCode: string) {
  if (roles.includes('SYSTEM_MAINTAINER')) {
    return true
  }
  if (requiredRoleCode === 'REGISTERED_USER') {
    return roles.length > 0
  }
  if (requiredRoleCode === 'CLUB_MEMBER') {
    return roles.some((role) => ['CLUB_MEMBER', 'DEPARTMENT_LEADER', 'PRESIDENT'].includes(role))
  }
  if (requiredRoleCode === 'DEPARTMENT_LEADER') {
    return roles.some((role) => ['DEPARTMENT_LEADER', 'PRESIDENT'].includes(role))
  }
  if (requiredRoleCode === 'PRESIDENT') {
    return roles.includes('PRESIDENT')
  }
  return roles.includes(requiredRoleCode)
}
