import { useEffect, useState } from 'react'
import { getCurrentUser, type UserProfile } from '@/api/modules/auth'
import { hasPermission } from '@/auth'

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
      {user && <p>当前角色：{user.roles.join('、') || '无'}</p>}
      {error && <p className="error">{error}</p>}
      {user && (
        <div className="admin-actions">
          {hasPermission(user, 'activity:review') && <button type="button">活动审核</button>}
          {hasPermission(user, 'member:manage') && <button type="button">成员管理</button>}
          {hasPermission(user, 'department:manage') && <button type="button">部门管理</button>}
          {hasPermission(user, 'system:maintain') && <button type="button">角色权限</button>}
        </div>
      )}
      <p>后台菜单和按钮会根据用户角色、权限动态显示。</p>
    </section>
  )
}
