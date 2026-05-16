import { useEffect, useState, type ReactNode } from 'react'
import { Navigate, createBrowserRouter } from 'react-router-dom'
import App from '@/App'
import ActivitiesView from '@/views/ActivitiesView'
import AdminView from '@/views/AdminView'
import HomeView from '@/views/HomeView'
import LoginView from '@/views/LoginView'
import { getCurrentUser, type UserProfile } from '@/api/modules/auth'
import { canAccessAdmin, getAccessToken, saveUser } from '@/auth'

function RequireAuth({ children }: { children: ReactNode }) {
  const token = getAccessToken()
  const [user, setUser] = useState<UserProfile | null>(null)
  const [loading, setLoading] = useState(Boolean(token))
  const [authorized, setAuthorized] = useState(false)

  useEffect(() => {
    if (!token) {
      return
    }
    let active = true
    getCurrentUser()
      .then((currentUser) => {
        if (!active) {
          return
        }
        saveUser(currentUser)
        setUser(currentUser)
        setAuthorized(canAccessAdmin(currentUser))
      })
      .catch(() => {
        if (active) {
          setAuthorized(false)
        }
      })
      .finally(() => {
        if (active) {
          setLoading(false)
        }
      })
    return () => {
      active = false
    }
  }, [token])

  if (!token) {
    return <Navigate to="/login?redirect=/admin" replace />
  }
  if (loading) {
    return <section className="content">正在验证权限...</section>
  }
  if (!user || !authorized) {
    return <Navigate to="/" replace />
  }
  return children
}

const router = createBrowserRouter([
  {
    path: '/',
    element: <App />,
    children: [
      {
        index: true,
        element: <HomeView />,
      },
      {
        path: 'login',
        element: <LoginView />,
      },
      {
        path: 'activities',
        element: <ActivitiesView />,
      },
      {
        path: 'admin',
        element: (
          <RequireAuth>
            <AdminView />
          </RequireAuth>
        ),
      },
    ],
  },
])

export default router
