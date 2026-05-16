import { Link, Outlet } from 'react-router-dom'

export default function App() {
  return (
    <main className="page">
      <header className="topbar">
        <strong>Club System Plus</strong>
        <nav>
          <Link to="/activities">活动</Link>
          <Link to="/login">登录</Link>
          <Link to="/admin">后台</Link>
        </nav>
      </header>
      <Outlet />
    </main>
  )
}
