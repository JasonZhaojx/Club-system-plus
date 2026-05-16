import { useEffect, useState } from 'react'
import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { logout } from '@/api/modules/auth'
import { canAccessAdmin, clearAuth, getStoredUser } from '@/auth'
import type { UserProfile } from '@/api/modules/auth'
import { TOAST_EVENT, type ToastPayload } from '@/toast'

export default function App() {
  const navigate = useNavigate()
  const [user, setUser] = useState<UserProfile | null>(() => getStoredUser())
  const [toast, setToast] = useState<ToastPayload | null>(null)
  const [sidebarCollapsed, setSidebarCollapsed] = useState(
    () => localStorage.getItem('sidebar_collapsed') === 'true',
  )

  useEffect(() => {
    function handleAuthChange() {
      setUser(getStoredUser())
    }
    window.addEventListener('auth-changed', handleAuthChange)
    window.addEventListener('storage', handleAuthChange)
    return () => {
      window.removeEventListener('auth-changed', handleAuthChange)
      window.removeEventListener('storage', handleAuthChange)
    }
  }, [])

  useEffect(() => {
    function handleToast(event: Event) {
      const detail = (event as CustomEvent<ToastPayload>).detail
      setToast({ message: detail.message, type: detail.type || 'error' })
    }
    window.addEventListener(TOAST_EVENT, handleToast)
    return () => {
      window.removeEventListener(TOAST_EVENT, handleToast)
    }
  }, [])

  useEffect(() => {
    if (!toast) {
      return
    }
    const timer = window.setTimeout(() => setToast(null), 2000)
    return () => window.clearTimeout(timer)
  }, [toast])

  async function handleLogout() {
    try {
      await logout()
    } catch {
      // The local session should be cleared even if the server token has expired.
    } finally {
      clearAuth()
      navigate('/login')
    }
  }

  function toggleSidebar() {
    setSidebarCollapsed((current) => {
      localStorage.setItem('sidebar_collapsed', String(!current))
      return !current
    })
  }

  return (
    <main className={sidebarCollapsed ? 'page sidebar-collapsed' : 'page'}>
      <header className="topbar">
        <div className="topbar-spacer" />
        <div className="account-area">
          <strong>Club System Plus</strong>
          {user ? (
            <>
              <NavLink className="nav-user" to="/profile">
                {user.nickname || user.username}
              </NavLink>
              <button className="link-button" onClick={handleLogout} type="button">
                退出
              </button>
            </>
          ) : (
            <NavLink to="/login">登录</NavLink>
          )}
        </div>
      </header>
      <button
        aria-label={sidebarCollapsed ? '展开侧栏' : '收起侧栏'}
        className="sidebar-toggle"
        onClick={toggleSidebar}
        type="button"
      >
        {sidebarCollapsed ? '›' : '‹'}
      </button>
      <aside className="sidebar">
        <nav className="side-nav">
          <NavLink to="/" end>
            首页
          </NavLink>
          <NavLink to="/about">社团介绍</NavLink>
          <NavLink to="/departments">部门展示</NavLink>
          <NavLink to="/leaders">重要成员</NavLink>
          <NavLink to="/activities">活动</NavLink>
          {user && <NavLink to="/profile">我的资料</NavLink>}
          {user && <NavLink to="/my-activities">我的活动</NavLink>}
          {canAccessAdmin(user) && <NavLink to="/admin">后台</NavLink>}
        </nav>
      </aside>
      <Outlet />
      {toast && (
        <div className={`toast toast-${toast.type || 'error'}`} role="alert">
          <span>{toast.message}</span>
          <button aria-label="关闭提示" onClick={() => setToast(null)} type="button">
            x
          </button>
        </div>
      )}
    </main>
  )
}
