import { useEffect } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import type { ReactNode } from 'react'

function PageHero({ title, subtitle, children }: { title: string; subtitle?: string; children?: ReactNode }) {
  return <section className="page-hero"><div className="container"><div className="page-hero-panel"><p className="eyebrow">UNSWCSA</p><h1>{title}</h1>{subtitle && <p className="muted">{subtitle}</p>}{children}</div></div></section>
}
function Photo({ portrait = false }: { portrait?: boolean }) {
  return <div className={`photo-slot${portrait ? ' portrait' : ''}`}><span>照片待提供</span></div>
}
function PreviewNote() {
  return <p className="preview-note">视觉开发预览 · 正式资料及链接待补充</p>
}
function Departments() {
  return <div className="three-grid">{[1, 2, 3].map(i => <article className="content-card" key={i}><Photo /><div className="content-card-copy"><h3>部门名称待补充</h3><p className="muted">部门职责与介绍待补充。</p></div></article>)}</div>
}

export function About() {
  return <><PageHero title="关于我们" subtitle="新南威尔士大学中国学生学者联谊会"><p className="english-name">University of New South Wales Chinese Student Association</p></PageHero>
    <div className="container page-body"><PreviewNote />
      {['学联定位', '愿景与使命', '简短历史', '组织联系'].map(title => <section className="page-section" key={title}><h2>{title}</h2><p className="muted">{title === '组织联系' ? '与 UNSW、Arc 等组织的正式联系待核实补充。' : '正式内容待补充。'}</p><div className="wide-photo"><Photo /></div></section>)}
      <section className="page-section"><h2>联系我们</h2><div className="two-grid">{['一般咨询', '商务合作'].map(title => <article className="info-panel" key={title}><h3>{title}</h3><p className="muted">官方邮箱待补充。</p></article>)}</div></section>
    </div></>
}

// These years demonstrate the approved layout, not verified team archives.
const previewYears = ['2026', '2025', '2024']
function useYearMonth(includeMonth: boolean) {
  const [params, setParams] = useSearchParams()
  const rawYear = params.get('year') || ''
  const rawMonth = params.get('month') || ''
  const year = /^[1-9]\d{3}$/.test(rawYear) ? rawYear : ''
  const month = /^(?:[1-9]|1[0-2])$/.test(rawMonth) ? rawMonth : ''
  useEffect(() => {
    const next = new URLSearchParams(params)
    if (params.has('year') && !year) next.delete('year')
    if (includeMonth && params.has('month') && !month) next.delete('month')
    if (next.toString() !== params.toString()) setParams(next, { replace: true })
  }, [params, setParams, year, month, includeMonth])
  const update = (key: string, value: string) => {
    const next = new URLSearchParams(params)
    if (value) next.set(key, value)
    else next.delete(key)
    setParams(next, { preventScrollReset: true })
  }
  const clear = () => {
    const next = new URLSearchParams(params)
    next.delete('year'); next.delete('month')
    setParams(next, { preventScrollReset: true })
  }
  return { year, month, update, clear }
}

export function Team() {
  const { year, update } = useYearMonth(false)
  return <><PageHero title="部门与团队" subtitle="UNSWCSA" /><div className="container page-body"><PreviewNote />
    <section className="page-section"><h2>部门介绍</h2><Departments /></section>
    <section className="page-section"><h2>{year ? `${year} 团队` : '本届团队'}</h2>
      <div className="year-switch" role="group" aria-label="团队年份"><button aria-pressed={!year} onClick={() => update('year', '')}>本届</button>{previewYears.map(value => <button key={value} aria-pressed={year === value} onClick={() => update('year', value)}>{value}</button>)}</div>
      <p className="muted" role="status">{year ? `${year} 年名单尚未接入。` : '本届展示年份与名单待确认。'}年份按钮仅供布局预览。</p>
      <div className="three-grid member-grid">{[1, 2, 3, 4, 5, 6].map(i => <article className="member-card" key={i}><Photo portrait /><div><h3>姓名待补充</h3><p className="muted">职位待补充</p></div></article>)}</div>
    </section>
  </div></>
}

/** Only verified Eventbrite links should be supplied; absent links are never clickable. */
export function EventbriteLink({ url }: { url: string | null }) {
  let valid = false
  try {
    const parsed = new URL(url || '')
    valid = parsed.protocol === 'https:' && /(^|\.)eventbrite\.(com|com\.au)$/.test(parsed.hostname) && !parsed.username && !parsed.password
  } catch { /* Unconfirmed URLs remain noninteractive. */ }
  return valid ? <a className="button" href={url!} target="_blank" rel="noopener noreferrer">前往 Eventbrite <span aria-hidden="true">↗</span><span className="sr-only">（新窗口打开）</span></a> : <span className="pending-link">Eventbrite 链接待补充</span>
}

export function BrandEvents() {
  return <><PageHero title="品牌活动" subtitle="已结束的代表性活动" /><div className="container page-body"><PreviewNote /><div className="two-grid event-grid">{[1, 2, 3, 4].map(i => <article className="content-card" key={i}><Photo /><div className="content-card-copy"><p className="eyebrow">品牌活动 · 内容预留</p><h2 className="card-title">品牌活动名称待补充</h2><p className="muted">代表性活动的简短介绍待补充。</p><EventbriteLink url={null} /></div></article>)}</div></div></>
}

export function Activities() {
  const { year, month, update, clear } = useYearMonth(true)
  return <><PageHero title="全部活动" subtitle="相聚于每一次活动" /><div className="container page-body"><PreviewNote />
    <div className="filter-bar"><label>年份<select value={year} onChange={e => update('year', e.target.value)}><option value="">全部年份</option>{[...new Set([...previewYears, ...(year ? [year] : [])])].sort().reverse().map(value => <option key={value}>{value}</option>)}</select></label><label>月份<select value={month} onChange={e => update('month', e.target.value)}><option value="">全部月份</option>{Array.from({ length: 12 }, (_, i) => <option value={i + 1} key={i}>{i + 1} 月</option>)}</select></label><button className="filter-reset" disabled={!year && !month} onClick={clear}>清除筛选</button></div>
    <p className="filter-notice" role="status">{year || month ? `已选择：${year ? `${year} 年` : '全部年份'} · ${month ? `${month} 月` : '全部月份'}。` : ''}活动数据尚未接入，以下为卡片布局预览，并非筛选结果。正式活动将在 Eventbrite 打开。</p>
    <div className="two-grid event-grid">{[1, 2, 3, 4].map(i => <article className="content-card" key={i}><div className="event-cover"><Photo /><span className="event-badge">内容预留</span></div><div className="content-card-copy"><h2 className="card-title">活动名称待补充</h2><dl className="event-meta"><div><dt>时间</dt><dd>待补充 · 悉尼时间</dd></div><div><dt>地点</dt><dd>待补充</dd></div></dl><p className="muted">活动简短介绍待补充。</p><EventbriteLink url={null} /></div></article>)}</div>
  </div></>
}

export function Join() {
  return <><PageHero title="加入我们" subtitle="招新信息待确认"><p className="muted">开放时间、开放部门与申请入口将于确认后公布。</p><a className="button button-red" href="#join-faq">查看常见问题 <span aria-hidden="true">↓</span></a></PageHero>
    <div className="container page-body"><PreviewNote /><section className="page-section"><h2>为什么加入</h2><p className="muted">加入理由与介绍文案待补充。</p><div className="wide-photo"><Photo /></div></section>
      <section className="page-section"><div className="section-heading"><h2>部门简介</h2><Link className="text-link" to="/team">认识我们的团队 ↗</Link></div><Departments /></section>
      <section className="page-section"><h2>申请流程</h2><ol className="three-grid process-list">{['01', '02', '03'].map(number => <li className="info-panel" key={number}><span className="step-number" aria-hidden="true">{number}</span><div><h3>流程待补充</h3><p className="muted">正式申请步骤待确认。</p></div></li>)}</ol></section>
      <section className="page-section" id="join-faq"><h2>常见问题</h2><div className="faq-list">{['什么时候可以申请？', '有哪些部门开放招新？', '如何提交申请？'].map(question => <details key={question}><summary>{question}</summary><p>相关信息待确认，请以之后公布的正式招新信息为准。</p></details>)}</div></section>
    </div></>
}
