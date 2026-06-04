import { useEffect, useState } from 'react'
import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { logout } from '@/api/modules/auth'
import { canAccessDashboard, clearAuth, getStoredUser } from '@/auth'
import type { UserProfile } from '@/api/modules/auth'
import { TOAST_EVENT, type ToastPayload } from '@/toast'
import AssistantWidget from '@/components/AssistantWidget'

const DEFAULT_AVATAR_URL =
  'https://ts1.tc.mm.bing.net/th/id/OIP-C.4n3KcdpOWTC32-U0LjDagwHaHa?cb=thfc1falcon&rs=1&pid=ImgDetMain&o=7&rm=3'

export default function App() {
  const navigate = useNavigate()
  const [user, setUser] = useState<UserProfile | null>(() => getStoredUser())
  const [toast, setToast] = useState<ToastPayload | null>(null)

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
    const timer = window.setTimeout(() => setToast(null), 3000)
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

  const toastType = toast?.type || 'error'

  return (
    <main className="page">
      <header className="topbar">
        <NavLink className="brand-link" to="/">
          UNSW CSA 新南学联
        </NavLink>
        <nav className="side-nav top-nav">
          <NavLink to="/" end>
            首页
          </NavLink>
          <NavLink to="/activities">活动</NavLink>
          <NavLink to="/departments">部门展示</NavLink>
          <NavLink to="/leaders">重要成员</NavLink>
          <NavLink to="/coupons">优惠券</NavLink>
          {user && <NavLink to="/my">我的</NavLink>}
          {canAccessDashboard(user) && <NavLink to="/dashboard">数据面板</NavLink>}
        </nav>
        <div className="account-area">
          <NavLink className="top-contact-button" to="/join">
            联系我们
          </NavLink>
          {user ? (
            <>
              <NavLink className="nav-user" to="/profile">
                <img
                  alt="个人资料"
                  className="nav-avatar"
                  src={user.avatarUrl || DEFAULT_AVATAR_URL}
                />
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
      <Outlet />
      <AssistantWidget />
      {toast && (
        <div className={`toast toast-${toastType}`} role="alert">
          <span className="toast-mark" aria-hidden="true" />
          <div className="toast-copy">
            <strong>{toastType === 'success' ? '操作成功' : '处理失败'}</strong>
            <span>{toast.message}</span>
          </div>
          <button aria-label="关闭提示" onClick={() => setToast(null)} type="button">
            x
          </button>
        </div>
      )}
    </main>
  )
}
