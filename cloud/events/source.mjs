export const MESSAGE = '活动信息暂时无法加载，请稍后重试'
const API = 'https://www.eventbriteapi.com/v3'
export async function getJson(path, token, fetcher = fetch, signal) {
  let response
  try {
    response = await fetcher(`${API}${path}`, { headers: { Authorization: `Bearer ${token}` }, signal, redirect: 'manual' })
  } catch (error) {
    const message = String(error?.message || '')
    const reason = /illegal invocation/i.test(message) ? 'FETCH_BINDING' : /header|ByteString|character/i.test(message) ? 'TOKEN_CHARACTERS' : /certificate|TLS|SSL/i.test(message) ? 'TLS' : /abort|timeout/i.test(message) ? 'TIMEOUT' : 'NETWORK_OR_RUNTIME'
    throw new Error(`Eventbrite request failed: ${reason}`)
  }
  if (!response.ok) throw new Error(`Eventbrite HTTP ${response.status}`)
  try { return await response.json() } catch { throw new Error('Eventbrite response is not JSON') }
}
export async function pages(path, key, token, fetcher = fetch) {
  const signal = AbortSignal.timeout(20000)
  const output = []; const seen = new Set(); let continuation
  for (let page = 0; page < 20; page++) {
    const suffix = continuation ? `${path.includes('?') ? '&' : '?'}continuation=${encodeURIComponent(continuation)}` : ''
    const data = await getJson(path + suffix, token, fetcher, signal)
    if (!Array.isArray(data[key]) || !data.pagination || typeof data.pagination.has_more_items !== 'boolean') throw new Error('Invalid pagination')
    output.push(...data[key])
    if (!data.pagination.has_more_items) return output
    continuation = data.pagination.continuation
    if (typeof continuation !== 'string' || !continuation || seen.has(continuation)) throw new Error('Invalid continuation')
    seen.add(continuation)
  }
  throw new Error('Pagination limit exceeded')
}
function https(value) {
  try { const url = new URL(value); return url.protocol === 'https:' && !url.username && !url.password ? url.href : null } catch { return null }
}
export function normalize(event, organization) {
  if (!event || typeof event !== 'object') throw new Error('Invalid public event')
  if (String(event.organization_id) !== organization || event.listed !== true || event.invite_only === true || event.is_series_parent === true || !['live', 'started', 'ended', 'completed', 'canceled'].includes(event.status)) return null
  if (!/^\d+$/.test(event.id) || !event.name?.text || !Number.isFinite(Date.parse(event.start?.utc)) || !Number.isFinite(Date.parse(event.end?.utc))) throw new Error('Invalid public event')
  const url = https(event.url)
  if (!url) throw new Error('Invalid registration URL')
  return { id: event.id, title: event.name.text, summary: typeof event.summary === 'string' ? event.summary : '', image: https(event.logo?.url), url,
    start: event.start.utc, end: event.end.utc, hideStart: event.hide_start_date === true, hideEnd: event.hide_end_date === true,
    status: event.status, venue: event.online_event ? '线上活动' : [event.venue?.name, event.venue?.address?.localized_address_display].filter(Boolean).join(' · ') || '地点待公布' }
}
export async function loadEvents(env, fetcher = fetch) {
  if (!env.EVENTBRITE_PRIVATE_TOKEN || !/^\d+$/.test(env.EVENTBRITE_ORGANIZATION_ID || '')) throw new Error('Missing configuration')
  const raw = await pages(`/organizations/${env.EVENTBRITE_ORGANIZATION_ID}/events/?expand=venue,logo&status=all`, 'events', env.EVENTBRITE_PRIVATE_TOKEN, fetcher)
  return [...new Map(raw.map(event => normalize(event, env.EVENTBRITE_ORGANIZATION_ID)).filter(Boolean).map(event => [event.id, event])).values()].sort((a, b) => Date.parse(b.start) - Date.parse(a.start))
}
