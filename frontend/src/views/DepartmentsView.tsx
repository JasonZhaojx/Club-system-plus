import { departments } from '@/data/publicContent'

export default function DepartmentsView() {
  return (
    <section className="content public-page">
      <div className="section-head">
        <div>
          <p className="profile-eyebrow">Departments</p>
          <h1>部门展示</h1>
          <p>不同部门分工协作，共同支撑迎新、文化活动、职业发展、宣传传播和对外合作。</p>
        </div>
      </div>
      <div className="value-grid department-grid">
        {departments.map((department) => (
          <section key={department.name}>
            <span>{department.focus}</span>
            <h2>{department.name}</h2>
            <p>{department.description}</p>
          </section>
        ))}
      </div>
    </section>
  )
}
