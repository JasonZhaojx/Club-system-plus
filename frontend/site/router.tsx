import { Link, Navigate, createBrowserRouter } from 'react-router-dom'
import Layout from './Layout'
import Home from './Home'
import { navigation } from './config'

function Placeholder({ title, description = '本页面布局与内容将在后续阶段完成。' }: { title: string; description?: string }) {
  return <section className="container placeholder-page"><p className="eyebrow">新版官网 · 建设中</p><h1>{title}</h1><p className="muted">{description}</p><Link className="button" to="/">返回首页 <span aria-hidden="true">↗</span></Link></section>
}

export const router = createBrowserRouter([
  { path: '/', element: <Layout />, children: [
    { index: true, element: <Home /> },
    ...navigation.slice(1).map(item => ({ path: item.path.slice(1), element: <Placeholder title={item.label} /> })),
    { path: 'brand-events/:slug', element: <Placeholder title="品牌活动详情" description="品牌活动数据尚未接入，当前不能确认此活动是否存在。" /> },
    { path: 'activities/:eventbriteId', element: <Placeholder title="活动详情" description="Eventbrite 数据尚未接入，当前不能确认此活动是否存在。" /> },
    { path: 'departments', element: <Navigate to="/team" replace /> },
    { path: 'leaders', element: <Navigate to="/team" replace /> },
    { path: 'admin', element: <Placeholder title="内容管理" description="新版后台入口尚未接入。此页面不提供登录、草稿读取或发布操作。" /> },
    { path: '*', element: <Placeholder title="页面不存在" description="请检查网址，或从导航选择要访问的页面。" /> },
  ] },
])
