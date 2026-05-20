import { useEffect, useState, type ReactNode } from 'react'
import { Navigate, createBrowserRouter } from 'react-router-dom'
import App from '@/App'
import AboutView from '@/views/AboutView'
import ActivityDetailView from '@/views/ActivityDetailView'
import ActivitiesView from '@/views/ActivitiesView'
import AdminView from '@/views/AdminView'
import CouponsView from '@/views/CouponsView'
import DashboardView from '@/views/DashboardView'
import DepartmentsView from '@/views/DepartmentsView'
import HomeView from '@/views/HomeView'
import LeadersView from '@/views/LeadersView'
import LoginView from '@/views/LoginView'
import MyActivitiesView from '@/views/MyActivitiesView'
import MyCouponsView from '@/views/MyCouponsView'
import MyView from '@/views/MyView'
import ProfileView from '@/views/ProfileView'
import { getCurrentUser, type UserProfile } from '@/api/modules/auth'
import { canAccessAdmin, canAccessDashboard, getAccessToken, saveUser } from '@/auth'
import PageLoading from '@/components/PageLoading'

function RequireAuth({
  children,
  redirect = '/admin',
  authorize = canAccessAdmin,
}: {
  children: ReactNode
  redirect?: string
  authorize?: (user: UserProfile | null) => boolean
}) {
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
        setAuthorized(authorize(currentUser))
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
  }, [authorize, token])

  if (!token) {
    return <Navigate to={`/login?redirect=${encodeURIComponent(redirect)}`} replace />
  }
  if (loading) {
    return <PageLoading />
  }
  if (!user || !authorized) {
    return <Navigate to="/" replace />
  }
  return children
}

function RequireLogin({ children, redirect = '/profile' }: { children: ReactNode; redirect?: string }) {
  const token = getAccessToken()

  if (!token) {
    return <Navigate to={`/login?redirect=${encodeURIComponent(redirect)}`} replace />
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
        path: 'coupons',
        element: (
          <RequireLogin redirect="/coupons">
            <CouponsView />
          </RequireLogin>
        ),
      },
      {
        path: 'activities/:activityId',
        element: <ActivityDetailView />,
      },
      {
        path: 'about',
        element: <AboutView />,
      },
      {
        path: 'departments',
        element: <DepartmentsView />,
      },
      {
        path: 'leaders',
        element: <LeadersView />,
      },
      {
        path: 'profile',
        element: (
          <RequireLogin redirect="/profile">
            <ProfileView />
          </RequireLogin>
        ),
      },
      {
        path: 'my',
        element: (
          <RequireLogin redirect="/my">
            <MyView />
          </RequireLogin>
        ),
      },
      {
        path: 'my-activities',
        element: (
          <RequireLogin redirect="/my-activities">
            <MyActivitiesView />
          </RequireLogin>
        ),
      },
      {
        path: 'my-coupons',
        element: (
          <RequireLogin redirect="/my-coupons">
            <MyCouponsView />
          </RequireLogin>
        ),
      },
      {
        path: 'admin',
        element: (
          <RequireAuth redirect="/admin">
            <AdminView />
          </RequireAuth>
        ),
      },
      {
        path: 'dashboard',
        element: (
          <RequireAuth authorize={canAccessDashboard} redirect="/dashboard">
            <DashboardView />
          </RequireAuth>
        ),
      },
    ],
  },
])

export default router
