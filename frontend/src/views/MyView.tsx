import { Link } from 'react-router-dom'
import { canAccessAdmin, getStoredUser } from '@/auth'

export default function MyView() {
  const user = getStoredUser()
  const myEntries = [
    {
      title: '我的活动',
      description: '查看已报名活动、报名状态和活动入口。',
      to: '/my-activities',
      meta: 'Activities',
    },
    {
      title: '我的券包',
      description: '查看已领取优惠券、使用状态和核销记录。',
      to: '/my-coupons',
      meta: 'Coupons',
    },
    ...(canAccessAdmin(user)
      ? [{
          title: '后台',
          description: '管理活动、优惠券、部门、成员和权限配置。',
          to: '/admin',
          meta: 'Admin',
        }]
      : []),
  ]

  return (
    <section className="content profile-page">
      <div className="section-head">
        <div>
          <p className="profile-eyebrow">My Space</p>
          <h1>我的</h1>
          <p>集中管理你的活动报名和优惠券。</p>
        </div>
      </div>

      <div className="my-entry-grid">
        {myEntries.map((entry) => (
          <Link className="my-entry-card" key={entry.to} to={entry.to}>
            <span>{entry.meta}</span>
            <h2>{entry.title}</h2>
            <p>{entry.description}</p>
          </Link>
        ))}
      </div>
    </section>
  )
}
