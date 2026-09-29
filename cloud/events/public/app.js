const errorText = '活动信息暂时无法加载，请稍后重试'
const root = document.querySelector('#events'), status = document.querySelector('#status')
const year = document.querySelector('#year'), month = document.querySelector('#month')
const id = location.pathname.match(/^\/events\/(\d+)$/)?.[1]
const date = new Intl.DateTimeFormat('zh-CN', { timeZone: 'Australia/Sydney', dateStyle: 'medium', timeStyle: 'short' })
const parts = value => Object.fromEntries(new Intl.DateTimeFormat('en', { timeZone: 'Australia/Sydney', year: 'numeric', month: '2-digit' }).formatToParts(new Date(value)).map(part => [part.type, part.value]))
for (let i = 1; i <= 12; i++) month.add(new Option(`${i}月`, String(i).padStart(2, '0')))
if (id) document.querySelector('#filters').hidden = true
let events = [], busy = false, failed = false
const labelFor = event => event.status === 'canceled' ? '已取消' : ['ended', 'completed'].includes(event.status) || Date.now() >= Date.parse(event.end) ? '已结束' : Date.now() >= Date.parse(event.start) ? '进行中' : '即将举行'
let statusUpdates = []
function node(tag, text) { const el = document.createElement(tag); el.textContent = text; return el }
function render() {
  root.replaceChildren()
  statusUpdates = []
  if (failed) return
  const selected = events.filter(event => id || ((!year.value || (!event.hideStart && parts(event.start).year === year.value)) && (!month.value || (!event.hideStart && parts(event.start).month === month.value))))
  if (!selected.length) { root.append(node('p', '暂无符合条件的活动')); return }
  for (const event of selected) {
    const card = document.createElement('article')
    const title = node('a', event.title); title.href = `/events/${event.id}`
    const heading = node('h2', ''); heading.append(title); card.append(heading)
    if (event.image) { const img = document.createElement('img'); img.src = event.image; img.alt = event.title; img.loading = 'lazy'; card.append(img) }
    const label = labelFor(event)
    const badge = node('p', label)
    card.append(badge, node('p', `${event.hideStart ? '时间待公布' : date.format(new Date(event.start))}${event.hideEnd ? '' : ` — ${date.format(new Date(event.end))}`}`), node('p', event.venue), node('p', event.summary || '暂无简短介绍，完整介绍请查看 Eventbrite。'))
    const link = node('a', label === '已取消' || label === '已结束' ? '在 Eventbrite 查看活动' : '前往 Eventbrite 报名'); link.href = event.url; link.target = '_blank'; link.rel = 'noopener noreferrer'; card.append(link); root.append(card)
    statusUpdates.push(() => { const current = labelFor(event); badge.textContent = current; link.textContent = ['已取消', '已结束'].includes(current) ? '在 Eventbrite 查看活动' : '前往 Eventbrite 报名' })
  }
}
async function refresh() {
  if (busy) return
  busy = true; status.textContent = '正在加载活动…'
  try {
    const response = await fetch(id ? `/api/events/${id}` : '/api/events', { cache: 'no-store', signal: AbortSignal.timeout(25000) })
    if (response.status === 404 && id) { failed = true; events = []; root.replaceChildren(); status.textContent = '活动不存在或未公开'; return }
    if (!response.ok) throw new Error()
    const data = await response.json(); events = id ? [data.event] : data.events; failed = false
    const previous = year.value; year.replaceChildren(new Option('全部', ''))
    for (const value of [...new Set(events.filter(e => !e.hideStart).map(e => parts(e.start).year))].sort().reverse()) year.add(new Option(value, value))
    year.value = previous; status.textContent = '已更新'; render()
  } catch { failed = true; events = []; root.replaceChildren(); status.textContent = errorText }
  finally { busy = false }
}
year.onchange = month.onchange = render
document.querySelector('#retry').onclick = refresh
setInterval(refresh, 60000)
setInterval(() => { if (!failed) statusUpdates.forEach(update => update()) }, 1000)
document.addEventListener('visibilitychange', () => { if (!document.hidden) refresh() })
refresh()
