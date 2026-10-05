import { Link, createBrowserRouter } from 'react-router-dom'

export const router = createBrowserRouter([
  {
    path: '/',
    element: (
      <main>
        <img className="site-logo" src="/brand/logos/unswcsa-horizontal.png" alt="新南学联 UNSWCSA" />
        <p className="eyebrow">新版官网 · 本地预览</p>
        <h1>独立入口已就绪</h1>
        <p>页面设计将在这里逐步实现。</p>
      </main>
    ),
  },
  {
    path: '*',
    element: (
      <main>
        <h1>页面不存在</h1>
        <p>新版页面尚在建设中。</p>
        <Link to="/">返回首页</Link>
      </main>
    ),
  },
])
