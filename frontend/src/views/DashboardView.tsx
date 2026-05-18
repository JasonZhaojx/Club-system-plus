import { useCallback, useEffect, useMemo, useState } from 'react'
import {
  getDashboardOverview,
  type ApiAccessLog,
  type ApiTrafficPoint,
  type DashboardConversion,
  type DashboardNameValue,
  type DashboardOverview,
  type DashboardRank,
  type OperationLog,
} from '@/api/modules/dashboard'
import { getErrorMessage } from '@/toast'

const statusLabels: Record<string, string> = {
  ACTIVE: '启用',
  DISABLED: '停用',
  REMOVED: '移除',
  DRAFT: '草稿',
  PENDING_REVIEW: '待审核',
  PUBLISHED: '已发布',
  CANCELLED: '已取消',
  FINISHED: '已结束',
  UNUSED: '未使用',
  USED: '已使用',
  EXPIRED: '已过期',
}

const chartColors = ['#73bf69', '#5794f2', '#f2cc0c', '#ff9830', '#f2495c', '#b877d9']

function formatDateTime(value?: string | null) {
  if (!value) {
    return '-'
  }
  return new Date(value).toLocaleString()
}

function labelOf(name: string) {
  return statusLabels[name] || name
}

function clampPercent(value: number) {
  return Math.max(0, Math.min(100, value))
}

function DonutChart({ data }: { data: DashboardNameValue[] }) {
  const total = data.reduce((sum, item) => sum + item.value, 0)
  let offset = 25

  if (!total) {
    return <div className="donut-empty">暂无数据</div>
  }

  return (
    <div className="donut-wrap compact">
      <svg className="donut-chart" viewBox="0 0 42 42" aria-label="状态占比">
        <circle className="donut-base" cx="21" cy="21" r="15.915" />
        {data.map((item, index) => {
          const percent = (item.value / total) * 100
          const circle = (
            <circle
              key={item.name}
              className="donut-slice"
              cx="21"
              cy="21"
              r="15.915"
              stroke={chartColors[index % chartColors.length]}
              strokeDasharray={`${percent} ${100 - percent}`}
              strokeDashoffset={offset}
            />
          )
          offset -= percent
          return circle
        })}
        <text className="donut-total" x="21" y="20.5">
          {total}
        </text>
        <text className="donut-caption" x="21" y="25">
          total
        </text>
      </svg>
      <div className="chart-legend">
        {data.map((item, index) => (
          <span key={item.name}>
            <i style={{ background: chartColors[index % chartColors.length] }} />
            {labelOf(item.name)} {item.value}
          </span>
        ))}
      </div>
    </div>
  )
}

function LineChart({ data }: { data: ApiTrafficPoint[] }) {
  const width = 760
  const height = 230
  const paddingLeft = 48
  const paddingRight = 24
  const paddingTop = 24
  const paddingBottom = 28
  const max = Math.max(...data.map((item) => item.total), 1)
  const yTicks = [max, Math.round(max * 0.66), Math.round(max * 0.33), 0]
  const points = data.map((item, index) => {
    const x = data.length <= 1
      ? paddingLeft
      : paddingLeft + (index / (data.length - 1)) * (width - paddingLeft - paddingRight)
    const y = height - paddingBottom - (item.total / max) * (height - paddingTop - paddingBottom)
    return { ...item, x, y }
  })
  const line = points.map((point) => `${point.x},${point.y}`).join(' ')
  const errorLine = points
    .map((point) => {
      const y = height - paddingBottom - (point.errorCount / max) * (height - paddingTop - paddingBottom)
      return `${point.x},${y}`
    })
    .join(' ')

  if (!data.length) {
    return <div className="chart-empty">最近 60 分钟暂无 API 访问</div>
  }

  return (
    <div className="line-chart-wrap">
      <svg className="line-chart" viewBox={`0 0 ${width} ${height}`} aria-label="API 访问趋势折线图">
        {yTicks.map((tick, row) => {
          const y = paddingTop + row * ((height - paddingTop - paddingBottom) / (yTicks.length - 1))
          return (
            <g key={`${tick}-${row}`}>
              <line className="chart-grid-line" x1={paddingLeft} x2={width - paddingRight} y1={y} y2={y} />
              <text className="y-axis-label" x={paddingLeft - 10} y={y + 4}>
                {tick}
              </text>
            </g>
          )
        })}
        <polyline className="line-chart-error" points={errorLine} />
        <polyline className="line-chart-main" points={line} />
        {points.map((point, index) => (
          <g key={`${point.bucket}-${index}`}>
            <circle className={point.errorCount ? 'line-point has-error' : 'line-point'} cx={point.x} cy={point.y} r="4" />
            {(index === 0 || index === points.length - 1 || index % 8 === 0) && (
              <text className="line-label" x={point.x} y={height - 7}>
                {point.bucket}
              </text>
            )}
          </g>
        ))}
      </svg>
      <div className="line-legend">
        <span><i className="legend-green" />访问量</span>
        <span><i className="legend-red" />异常量</span>
      </div>
    </div>
  )
}

function RankList({
  items,
  unit,
  empty,
}: {
  items: DashboardRank[]
  unit: string
  empty: string
}) {
  const max = Math.max(...items.map((item) => item.value), 1)

  return (
    <div className="rank-list">
      {items.map((item, index) => (
        <div className="rank-row" key={`${item.name}-${index}`}>
          <span className="rank-index">{index + 1}</span>
          <div className="rank-main">
            <div className="rank-label">
              <strong title={item.name}>{item.name}</strong>
              <em>{item.detail || ''}</em>
            </div>
            <span className="rank-track">
              <i style={{ width: `${Math.max((item.value / max) * 100, 3)}%` }} />
            </span>
          </div>
          <b>{item.value}{unit}</b>
        </div>
      ))}
      {!items.length && <div className="rank-empty">{empty}</div>}
    </div>
  )
}

function ConversionList({ items }: { items: DashboardConversion[] }) {
  return (
    <div className="conversion-list">
      {items.map((item) => (
        <div className="conversion-row" key={item.name}>
          <div className="conversion-head">
            <strong title={item.name}>{item.name}</strong>
            <span>{item.rate.toFixed(1)}%</span>
          </div>
          <div className="conversion-track">
            <i style={{ width: `${clampPercent(item.rate)}%` }} />
          </div>
          <p>{item.current.toLocaleString()} / {item.target.toLocaleString()}</p>
        </div>
      ))}
      {!items.length && <div className="rank-empty">暂无转化数据</div>}
    </div>
  )
}

function ApiLogTable({ logs }: { logs: ApiAccessLog[] }) {
  return (
    <div className="data-table-wrap dashboard-table">
      <table className="data-table">
        <thead>
          <tr>
            <th>时间</th>
            <th>方法</th>
            <th>路径</th>
            <th>状态</th>
            <th>耗时</th>
            <th>用户</th>
          </tr>
        </thead>
        <tbody>
          {logs.slice(0, 10).map((log) => (
            <tr key={log.id}>
              <td>{formatDateTime(log.createdAt)}</td>
              <td>{log.method}</td>
              <td>{log.path}</td>
              <td>
                <span className={log.statusCode >= 400 ? 'status-pill danger' : 'status-pill ok'}>
                  {log.statusCode}
                </span>
              </td>
              <td>{log.durationMs} ms</td>
              <td>{log.username || '-'}</td>
            </tr>
          ))}
          {!logs.length && (
            <tr>
              <td colSpan={6}>暂无访问日志</td>
            </tr>
          )}
        </tbody>
      </table>
    </div>
  )
}

function OperationLogTable({ logs }: { logs: OperationLog[] }) {
  return (
    <div className="data-table-wrap dashboard-table">
      <table className="data-table">
        <thead>
          <tr>
            <th>时间</th>
            <th>操作</th>
            <th>路径</th>
            <th>用户</th>
            <th>状态</th>
            <th>耗时</th>
          </tr>
        </thead>
        <tbody>
          {logs.slice(0, 10).map((log) => (
            <tr key={log.id}>
              <td>{formatDateTime(log.createdAt)}</td>
              <td>{log.action}</td>
              <td>{log.method} {log.path}</td>
              <td>{log.username || '-'}</td>
              <td>
                <span className={log.statusCode >= 400 ? 'status-pill danger' : 'status-pill ok'}>
                  {log.statusCode}
                </span>
              </td>
              <td>{log.durationMs} ms</td>
            </tr>
          ))}
          {!logs.length && (
            <tr>
              <td colSpan={6}>暂无操作日志</td>
            </tr>
          )}
        </tbody>
      </table>
    </div>
  )
}

export default function DashboardView() {
  const [overview, setOverview] = useState<DashboardOverview | null>(null)
  const [loading, setLoading] = useState(true)
  const [refreshing, setRefreshing] = useState(false)
  const [error, setError] = useState('')

  const loadOverview = useCallback(async (quiet = false) => {
    if (quiet) {
      setRefreshing(true)
    } else {
      setLoading(true)
    }
    setError('')
    try {
      setOverview(await getDashboardOverview())
    } catch (err) {
      setError(getErrorMessage(err, '数据面板加载失败'))
    } finally {
      setLoading(false)
      setRefreshing(false)
    }
  }, [])

  useEffect(() => {
    void loadOverview()
    const timer = window.setInterval(() => void loadOverview(true), 60000)
    return () => window.clearInterval(timer)
  }, [loadOverview])

  const avgDuration = useMemo(() => {
    if (!overview?.apiTraffic.length) {
      return 0
    }
    const points = overview.apiTraffic.filter((item) => item.avgDurationMs !== null)
    if (!points.length) {
      return 0
    }
    return Math.round(points.reduce((sum, item) => sum + (item.avgDurationMs || 0), 0) / points.length)
  }, [overview])

  if (loading && !overview) {
    return <section className="content dashboard-page">正在加载数据面板...</section>
  }

  return (
    <section className="content dashboard-page dashboard-wide">
      <div className="dashboard-hero compact">
        <div>
          <p className="profile-eyebrow">Grafana-style Dashboard</p>
          <h1>系统运行与业务数据</h1>
          <p>最近刷新：{formatDateTime(overview?.refreshedAt)} · 自动刷新：60 秒</p>
        </div>
        <button disabled={refreshing} onClick={() => void loadOverview(true)} type="button">
          {refreshing ? '刷新中...' : '手动刷新'}
        </button>
      </div>

      {error && <p className="form-error">{error}</p>}

      <div className="metric-grid horizontal">
        {overview?.metrics.map((metric) => (
          <article className={`metric-card tone-${metric.tone}`} key={metric.label}>
            <span>{metric.label}</span>
            <strong>{metric.value.toLocaleString()}</strong>
          </article>
        ))}
        <article className="metric-card tone-red">
          <span>平均响应耗时</span>
          <strong>{avgDuration} ms</strong>
        </article>
      </div>

      <div className="dashboard-layout">
        <article className="dashboard-panel trend-panel">
          <div className="panel-title">
            <h2>API 访问趋势</h2>
            <span>最近 60 分钟 · 折线图</span>
          </div>
          <LineChart data={overview?.apiTraffic || []} />
        </article>

        <article className="dashboard-panel">
          <div className="panel-title">
            <h2>热门接口排行</h2>
            <span>24h</span>
          </div>
          <RankList empty="暂无接口访问" items={overview?.hotApis || []} unit="次" />
        </article>

        <article className="dashboard-panel">
          <div className="panel-title">
            <h2>慢接口排行</h2>
            <span>平均耗时</span>
          </div>
          <RankList empty="暂无慢接口数据" items={overview?.slowApis || []} unit="ms" />
        </article>

        <article className="dashboard-panel">
          <div className="panel-title">
            <h2>异常日志排行</h2>
            <span>4xx / 5xx</span>
          </div>
          <RankList empty="暂无异常日志" items={overview?.errorApis || []} unit="次" />
        </article>

        <article className="dashboard-panel">
          <div className="panel-title">
            <h2>活跃用户排行</h2>
            <span>7d</span>
          </div>
          <RankList empty="暂无活跃用户" items={overview?.activeUsers || []} unit="次" />
        </article>

        <article className="dashboard-panel">
          <div className="panel-title">
            <h2>活动参与转化率</h2>
            <span>报名 / 容量</span>
          </div>
          <ConversionList items={overview?.activityConversions || []} />
        </article>

        <article className="dashboard-panel">
          <div className="panel-title">
            <h2>优惠券领取转化率</h2>
            <span>领取 / 库存</span>
          </div>
          <ConversionList items={overview?.couponConversions || []} />
        </article>

        <article className="dashboard-panel">
          <div className="panel-title">
            <h2>活动状态</h2>
            <span>活动数</span>
          </div>
          <DonutChart data={overview?.activityStatus || []} />
        </article>

        <article className="dashboard-panel">
          <div className="panel-title">
            <h2>成员状态</h2>
            <span>成员数</span>
          </div>
          <DonutChart data={overview?.memberStatus || []} />
        </article>

        <article className="dashboard-panel">
          <div className="panel-title">
            <h2>优惠券状态</h2>
            <span>领取数</span>
          </div>
          {null}
        </article>
      </div>

      <div className="dashboard-log-grid">
        <article className="dashboard-panel">
          <div className="panel-title">
            <h2>API 访问日志</h2>
            <span>最新 10 条</span>
          </div>
          <ApiLogTable logs={overview?.apiLogs || []} />
        </article>

        <article className="dashboard-panel">
          <div className="panel-title">
            <h2>操作日志</h2>
            <span>非 GET 成功请求</span>
          </div>
          <OperationLogTable logs={overview?.operationLogs || []} />
        </article>
      </div>
    </section>
  )
}
