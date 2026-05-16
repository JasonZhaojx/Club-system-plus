import type { ReactNode } from 'react'
import { Navigate, createBrowserRouter } from 'react-router-dom'
import App from '@/App'
import ActivitiesView from '@/views/ActivitiesView'
import AdminView from '@/views/AdminView'
import HomeView from '@/views/HomeView'
import LoginView from '@/views/LoginView'

function RequireAuth({ children }: { children: ReactNode }) {
  const token = localStorage.getItem('access_token')
  if (!token) {
    return <Navigate to="/login" replace />
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
