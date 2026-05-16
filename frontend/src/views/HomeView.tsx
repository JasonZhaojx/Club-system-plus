import { useState } from 'react'
import { getHealth, type HealthInfo } from '@/api/modules/system'

export default function HomeView() {
  const [health, setHealth] = useState<HealthInfo | null>(null)
  const [error, setError] = useState('')

  async function loadHealth() {
    setError('')
    try {
      setHealth(await getHealth())
    } catch {
      setError('无法连接后端服务')
    }
  }

  return (
    <section className="content">
      <h1>社团官网与活动运营管理系统</h1>
      <p>当前前端已切换为 React、TypeScript、Vite，并完成 Axios 与路由基础结构初始化。</p>
      <button type="button" onClick={loadHealth}>检查后端连接</button>
      {health && <p>后端状态：{health.status}，时间：{health.time}</p>}
      {error && <p className="error">{error}</p>}
    </section>
  )
}
