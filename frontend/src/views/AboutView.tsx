export default function AboutView() {
  return (
    <section className="content public-page">
      <div className="public-band">
        <p className="profile-eyebrow">About</p>
        <h1>我们把社团运营成一个真实的产品组织</h1>
        <p>
          Club System Plus 关注的不只是活动数量，而是活动背后的组织能力、项目质量和成员成长。
          社团采用部门协同、项目复盘和权限管理机制，让每位成员都能在明确的责任边界中参与真实交付。
        </p>
      </div>
      <div className="value-grid">
        <section>
          <h2>真实项目</h2>
          <p>围绕校园服务、AI 工具、活动系统和数据分析持续构建可展示项目。</p>
        </section>
        <section>
          <h2>高质量活动</h2>
          <p>每场活动都有目标、流程、负责人和复盘指标，避免松散的一次性组织。</p>
        </section>
        <section>
          <h2>长期成长</h2>
          <p>通过部门、角色和权限机制，让成员逐步承担更复杂的协作责任。</p>
        </section>
      </div>
    </section>
  )
}
