import { useEffect, useState } from 'react'
import { getCurrentUser, type UserProfile } from '@/api/modules/auth'

export default function AdminView() {
  const [user, setUser] = useState<UserProfile | null>(null)
  const [error, setError] = useState('')

  useEffect(() => {
    getCurrentUser()
      .then(setUser)
      .catch((err) => setError(err instanceof Error ? err.message : '用户信息加载失败'))
  }, [])

  return (
    <section className="content">
      <h1>管理后台</h1>
      {user && <p>当前登录用户：{user.nickname || user.username}</p>}
      {error && <p className="error">{error}</p>}
      <p>该路由已配置登录守卫，可继续扩展审核、成员、部门和数据面板页面。</p>
    </section>
  )
}
