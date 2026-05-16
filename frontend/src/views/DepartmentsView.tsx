import { departments } from '@/data/publicContent'

export default function DepartmentsView() {
  return (
    <section className="content public-page">
      <div className="section-head">
        <div>
          <p className="profile-eyebrow">Departments</p>
          <h1>部门展示</h1>
          <p>不同部门承担不同组织职责，共同支撑活动、项目和成员发展。</p>
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
