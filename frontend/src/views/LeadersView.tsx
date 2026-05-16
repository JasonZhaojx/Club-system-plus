import { leaders } from '@/data/publicContent'

export default function LeadersView() {
  return (
    <section className="content public-page">
      <div className="section-head">
        <div>
          <p className="profile-eyebrow">Leadership</p>
          <h1>重要成员</h1>
          <p>核心成员负责社团方向、活动质量、部门协作和对外资源连接。</p>
        </div>
      </div>
      <div className="leader-grid">
        {leaders.map((leader) => (
          <section key={leader.name}>
            <div>{leader.name.slice(0, 1)}</div>
            <h2>{leader.name}</h2>
            <span>{leader.role}</span>
            <p>{leader.area}</p>
          </section>
        ))}
      </div>
    </section>
  )
}
