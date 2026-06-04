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
    <section className="content public-page home-page">
      <div className="public-hero">
        <div className="public-hero-copy">
          <p className="profile-eyebrow">UNSW CSA · 澳大利亚中国大使馆官方认证</p>
          <h1>新南威尔士大学中国学联</h1>
          <p>
            自 1992 年成立至今，新南学联已走过三十余载春秋。我们始终秉持“家，因为有你”的理念，
            为每一位远渡重洋的学子打造一片归属之地。
          </p>
          <div className="hero-actions">
            <Link className="primary-link" to="/activities">
              浏览活动
            </Link>
            <Link className="secondary-link" to="/about">
              了解学联
            </Link>
            <Link className="secondary-link" to="/join">
              加入我们
            </Link>
          </div>
        </div>
      </div>

      <section className="metric-strip">
        <div>
          <strong>7千+</strong>
          <span>年度会员</span>
        </div>
        <div>
          <strong>200+</strong>
          <span>年度活动</span>
        </div>
        <div>
          <strong>4万+</strong>
          <span>公众号订阅</span>
        </div>
        <div>
          <strong>35万+</strong>
          <span>小红书阅读</span>
        </div>
      </section>

      <section className="home-activity-section">
        <div className="home-activity-intro">
          <p className="profile-eyebrow">Activities</p>
          <h2>丰富多彩的校园生活</h2>
          <p>从开学迎新到年终盛典，从学术拓展到文化传承，每一个时刻我们都陪伴在你身边。</p>
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
