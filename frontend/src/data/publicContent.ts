export type ActivityCategory = 'technology' | 'career' | 'community' | 'competition'

export interface PublicActivity {
  id: string
  title: string
  category: ActivityCategory
  categoryName: string
  date: string
  location: string
  summary: string
  detail: string
  image: string
  status: 'past' | 'upcoming'
  capacity: number
}

export const publicActivities: PublicActivity[] = [
  {
    id: 'ai-workshop-2026',
    title: 'AI Product Workshop',
    category: 'technology',
    categoryName: '技术工作坊',
    date: '2026-04-18',
    location: 'Innovation Hub A201',
    summary: '从需求洞察、原型设计到 RAG 应用落地，完成一套 AI 产品方案。',
    detail:
      '本次工作坊围绕 AI 应用从 0 到 1 的完整流程展开。成员分组完成用户问题定义、知识库设计、Prompt 评审和前端原型演示。活动最后由导师点评方案完整度、可行性和产品体验。',
    image: 'https://images.unsplash.com/photo-1519389950473-47ba0277781c?auto=format&fit=crop&w=1200&q=80',
    status: 'past',
    capacity: 80,
  },
  {
    id: 'hack-night',
    title: 'Hack Night',
    category: 'competition',
    categoryName: '挑战赛',
    date: '2026-03-22',
    location: 'Engineering Lab',
    summary: '一晚完成组队、开发、路演，用真实约束训练工程交付能力。',
    detail:
      'Hack Night 以限时交付为核心，题目覆盖校园服务、活动推荐、学习助手和社团运营工具。现场提供技术导师支持，最终以产品演示、代码质量和用户价值三个维度评选优秀团队。',
    image: 'https://images.unsplash.com/photo-1504384308090-c894fdcc538d?auto=format&fit=crop&w=1200&q=80',
    status: 'past',
    capacity: 120,
  },
  {
    id: 'career-panel',
    title: 'Career Panel',
    category: 'career',
    categoryName: '职业发展',
    date: '2026-02-28',
    location: 'Business School Theatre',
    summary: '邀请互联网、咨询和金融科技校友分享实习路径与项目经历。',
    detail:
      'Career Panel 聚焦简历准备、项目表达、面试复盘和行业选择。嘉宾来自大型互联网企业、创业团队和金融科技公司，分享从学生项目到真实业务项目的迁移方法。',
    image: 'https://images.unsplash.com/photo-1551836022-d5d88e9218df?auto=format&fit=crop&w=1200&q=80',
    status: 'past',
    capacity: 160,
  },
  {
    id: 'community-day',
    title: 'Community Day',
    category: 'community',
    categoryName: '社群活动',
    date: '2026-01-20',
    location: 'Campus Green',
    summary: '开放式社群日，展示部门项目、迎新交流和社团年度计划。',
    detail:
      'Community Day 面向所有学生开放。技术部、运营部、设计部和外联部展示过往项目成果，并提供部门咨询。新成员可以现场了解加入流程和后续活动安排。',
    image: 'https://images.unsplash.com/photo-1523580494863-6f3031224c94?auto=format&fit=crop&w=1200&q=80',
    status: 'past',
    capacity: 220,
  },
  {
    id: 'cloud-bootcamp',
    title: 'Cloud Bootcamp',
    category: 'technology',
    categoryName: '技术工作坊',
    date: '2025-11-16',
    location: 'Computing Lab G12',
    summary: '围绕云部署、CI/CD、监控和日志完成一次真实上线演练。',
    detail:
      '活动覆盖 Docker、Nginx、GitHub Actions、服务监控和回滚策略。成员以小组为单位部署一个前后端分离项目，并完成可观测性配置和故障演练。',
    image: 'https://images.unsplash.com/photo-1451187580459-43490279c0fa?auto=format&fit=crop&w=1200&q=80',
    status: 'past',
    capacity: 90,
  },
  {
    id: 'design-sprint',
    title: 'Design Sprint',
    category: 'community',
    categoryName: '社群活动',
    date: '2025-10-12',
    location: 'Design Studio',
    summary: '用一整天完成问题拆解、体验地图、低保真原型和用户测试。',
    detail:
      'Design Sprint 将产品、设计和工程成员混合组队。每组选择一个校园真实问题，完成从用户访谈到可点击原型的完整流程，并在活动末尾进行可用性测试。',
    image: 'https://images.unsplash.com/photo-1552664730-d307ca884978?auto=format&fit=crop&w=1200&q=80',
    status: 'past',
    capacity: 70,
  },
  {
    id: 'algorithm-arena',
    title: 'Algorithm Arena',
    category: 'competition',
    categoryName: '挑战赛',
    date: '2025-09-05',
    location: 'Online',
    summary: '算法竞赛训练夜，覆盖图论、动态规划和工程化题解复盘。',
    detail:
      'Algorithm Arena 面向希望提升算法能力的成员。活动分为热身题、限时赛和讲解复盘三个阶段，强调思路表达和复杂度分析。',
    image: 'https://images.unsplash.com/photo-1515879218367-8466d910aaa4?auto=format&fit=crop&w=1200&q=80',
    status: 'past',
    capacity: 100,
  },
  {
    id: 'startup-talk',
    title: 'Startup Talk',
    category: 'career',
    categoryName: '职业发展',
    date: '2025-08-14',
    location: 'Founders Space',
    summary: '创业校友分享产品验证、早期团队和技术决策的真实经验。',
    detail:
      '活动邀请创业校友分享从校园 idea 到真实产品的过程，包括用户访谈、MVP 搭建、融资前准备和工程团队协作。',
    image: 'https://images.unsplash.com/photo-1556761175-b413da4baf72?auto=format&fit=crop&w=1200&q=80',
    status: 'past',
    capacity: 130,
  },
]

export const departments = [
  {
    name: '技术部',
    focus: '工程研发、AI 应用、云部署和内部工具建设',
    description: '负责社团技术项目、课程工作坊和工程实践训练，沉淀可复用的项目模板和技术文档。',
  },
  {
    name: '运营部',
    focus: '活动策划、报名流程、现场执行和数据复盘',
    description: '负责活动排期、参与体验、社群沟通和活动效果分析，确保每场活动稳定落地。',
  },
  {
    name: '设计部',
    focus: '品牌视觉、交互原型、海报和官网体验',
    description: '负责社团视觉体系、活动物料和产品原型设计，让项目表达更清晰、更专业。',
  },
  {
    name: '外联部',
    focus: '校友连接、企业合作、资源拓展和嘉宾邀请',
    description: '负责连接校友、企业和校园组织，为成员争取更好的活动资源和职业机会。',
  },
]

export const leaders = [
  { name: 'Alex Chen', role: '社长', area: '组织战略与跨部门协同' },
  { name: 'Mia Zhang', role: '技术负责人', area: 'AI 应用与工程训练' },
  { name: 'Ryan Liu', role: '运营负责人', area: '活动增长与用户体验' },
  { name: 'Sophie Wang', role: '设计负责人', area: '品牌系统与产品原型' },
]
