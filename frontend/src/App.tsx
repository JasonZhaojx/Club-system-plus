import { useEffect, useState } from 'react'
import { Link, Outlet, useNavigate } from 'react-router-dom'
import { logout } from '@/api/modules/auth'
import { canAccessAdmin, clearAuth, getStoredUser } from '@/auth'
import type { UserProfile } from '@/api/modules/auth'

export default function App() {
  const navigate = useNavigate()
  const [user, setUser] = useState<UserProfile | null>(() => getStoredUser())

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

  return (
    <main className="page">
      <header className="topbar">
        <strong>Club System Plus</strong>
        <nav>
          <Link to="/activities">活动</Link>
          {user ? (
            <>
              <span className="nav-user">{user.nickname || user.username}</span>
              <button className="link-button" onClick={handleLogout} type="button">
                退出
              </button>
            </>
          ) : (
            <Link to="/login">登录</Link>
          )}
          {canAccessAdmin(user) && <Link to="/admin">后台</Link>}
        </nav>
      </header>
      <Outlet />
    </main>
  )
}
