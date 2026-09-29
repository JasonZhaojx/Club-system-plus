import { loadEvents, MESSAGE } from './source.mjs'
export function createWorker(fetcher = (...args) => globalThis.fetch(...args), now = Date.now) {
  let entry; let pending; let config
  return { async fetch(request, env) {
    const path = new URL(request.url).pathname
    const json = (body, status = 200) => Response.json(body, { status, headers: { 'Cache-Control': 'no-store', 'X-Content-Type-Options': 'nosniff' } })
    if (!['GET', 'HEAD'].includes(request.method)) return json({ error: 'Method not allowed' }, 405)
    if (!path.startsWith('/api/')) return env.ASSETS.fetch(request)
    const match = path.match(/^\/api\/events(?:\/(\d+))?$/)
    if (!match) return json({ error: 'Not found' }, 404)
    // Isolate-local request coalescing only; no persistent stale snapshot.
    const key = `${env.EVENTBRITE_ORGANIZATION_ID}:${env.EVENTBRITE_PRIVATE_TOKEN}`
    if (config !== key) { config = key; entry = null; pending = null }
    if (!entry || entry.until <= now()) {
      if (!pending) pending = loadEvents(env, fetcher).then(events => ({ events, until: now() + 30000 })).catch(error => {
        // Log only known diagnostics, never upstream bodies, URLs or credentials.
        const known = ['Eventbrite response is not JSON', 'Eventbrite request failed: FETCH_BINDING', 'Eventbrite request failed: TOKEN_CHARACTERS', 'Eventbrite request failed: TLS', 'Eventbrite request failed: TIMEOUT', 'Eventbrite request failed: NETWORK_OR_RUNTIME', 'Missing configuration', 'Invalid pagination', 'Invalid continuation', 'Pagination limit exceeded', 'Invalid public event', 'Invalid registration URL', 'AbortSignal.timeout is not a function']
        const reason = known.includes(error?.message) || /^Eventbrite HTTP \d{3}$/.test(error?.message || '') ? error.message : ['TimeoutError', 'AbortError', 'TypeError'].includes(error?.name) ? error.name : 'Unexpected upstream failure'
        console.error('[Eventbrite]', reason)
        return { error: MESSAGE, until: now() + 10000 }
      }).finally(() => { pending = null })
      entry = await pending
    }
    if (entry.error) return json({ error: MESSAGE }, 503)
    if (match[1]) {
      const event = entry.events.find(item => item.id === match[1])
      return event ? json({ event }) : json({ error: '活动不存在或未公开' }, 404)
    }
    return json({ events: entry.events })
  } }
}
export default createWorker()
