import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { listActivities, type Activity } from '@/api/modules/activity'

const fallbackImage =
  'https://images.unsplash.com/photo-1521737604893-d14cc237f11d?auto=format&fit=crop&w=1200&q=80'

export default function HomeView() {
  const [featuredActivities, setFeaturedActivities] = useState<Activity[]>([])

  useEffect(() => {
    let active = true
    listActivities({ page: 1, size: 4, sort: 'latest' })
      .then((result) => {
        if (active) {
          setFeaturedActivities(result.records)
        }
      })
      .catch(() => {
        if (active) {
          setFeaturedActivities([])
        }
      })
    return () => {
      active = false
    }
  }, [])

  return (
    <section className="content public-page">
      <div className="public-hero">
        <div className="public-hero-copy">
          <p className="profile-eyebrow">Club System Plus</p>
          <h1>连接校园人才、真实项目与高质量活动体验</h1>
          <p>
            面向技术、产品、设计和运营方向的综合型学生社团。我们用企业级项目方法组织活动，
            让成员在真实协作中积累作品、经验和长期伙伴关系。
          </p>
          <div className="hero-actions">
            <Link className="primary-link" to="/activities">
              浏览活动
            </Link>
            <Link className="secondary-link" to="/about">
              了解社团
            </Link>
          </div>
        </div>
      </div>

      <section className="metric-strip">
        <div>
          <strong>40+</strong>
          <span>年度活动</span>
        </div>
        <div>
          <strong>800+</strong>
          <span>参与人次</span>
        </div>
        <div>
          <strong>4</strong>
          <span>核心部门</span>
        </div>
        <div>
          <strong>12+</strong>
          <span>合作嘉宾</span>
        </div>
      </section>

      <section className="home-activity-section">
        <div className="home-activity-intro">
          <p className="profile-eyebrow">Past Events</p>
          <h2>过往活动</h2>
          <p>浏览已经完成的重点活动，查看主题、形式和复盘内容。</p>
          <Link className="view-all-link" to="/activities">
            查看全部活动
          </Link>
        </div>

        <div className="activity-grid">
          {featuredActivities.map((activity) => (
            <Link className="activity-card" key={activity.id} to={`/activities/${activity.id}`}>
              <img alt={activity.title} src={activity.imageUrl || fallbackImage} />
              <div>
                <span>{activity.categoryName}</span>
                <h3>{activity.title}</h3>
                <p>{activity.summary}</p>
              </div>
            </Link>
          ))}
        </div>
      </section>
    </section>
  )
}
