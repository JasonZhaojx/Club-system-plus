import { HttpError } from './github.mjs'

export function validateInput(input, action) {
  const keys = action === 'draft' ? ['revision', 'introduction'] : ['revision']
  if (!input || typeof input !== 'object' || Array.isArray(input) || Object.keys(input).some(key => !keys.includes(key))) throw new HttpError(400, '包含不允许的字段。')
  if (!/^[a-f0-9]{40}$/.test(input.revision || '')) throw new HttpError(400, '版本号无效。')
  if (action === 'draft' && (typeof input.introduction !== 'string' || !input.introduction.trim() || input.introduction.length > 600)) throw new HttpError(400, '简介必须为 1–600 个字符。')
}

export async function changeContent(github, action, input, { publicOrigin, fetcher = fetch, now = Date.now } = {}) {
  validateInput(input, action)
  const current = await github.read()
  if (input.revision !== current.revision) throw new HttpError(409, '内容已更新，请先复制当前输入，再重新加载。')
  const state = structuredClone(current.state)
  if (action === 'draft') {
    state.draft = { introduction: input.introduction.trim() }
    return github.write(current.revision, state, 'content: save home draft')
  }
  if (!['publish', 'retry'].includes(action)) throw new HttpError(404, '接口不存在。')
  if (action === 'retry' && !state.release) throw new HttpError(400, '没有可重试的发布。')
  const codeSha = await github.codeHead()
  if (action === 'retry' && state.release.codeSha !== codeSha) throw new HttpError(409, '页面代码已更新，请重新发布当前草稿。')
  const same = state.release && state.release.codeSha === codeSha &&
    (action === 'retry' || state.release.content.introduction === state.draft.introduction)
  if (same) {
    // A committed claim covers the delay before GitHub exposes a dispatched run.
    const claimedAt = Date.parse(state.release.dispatchRequestedAt || state.release.requestedAt)
    if (now() - claimedAt < 120000) return { ...current, reused: true }
    const status = await releaseStatus(github, state.release, publicOrigin, fetcher)
    if (['live', 'publishing', 'verifying'].includes(status.status)) return { ...current, reused: true }
    if (action === 'publish') return { ...current, reused: true, retryRequired: true }
  } else {
    state.release = { id: crypto.randomUUID(), content: { introduction: state.draft.introduction }, codeSha, requestedAt: new Date(now()).toISOString() }
  }
  // Contents API SHA compare-and-swap is shared across Worker instances.
  // Persist BEFORE dispatch: concurrent requests cannot both acquire the claim.
  state.release.dispatchRequestedAt = new Date(now()).toISOString()
  state.release.dispatchAttempt = (state.release.dispatchAttempt || 0) + 1
  const saved = await github.write(current.revision, state, `content: request release ${state.release.id}`)
  try { await github.dispatch(state.release.id) }
  catch { return { ...saved, dispatchFailed: true } }
  return saved
}

export async function releaseStatus(github, release, publicOrigin, fetcher = fetch) {
  if (!release) return { status: 'none' }
  try {
    const response = await fetcher(`${publicOrigin}/published.json?release=${release.id}`, { cache: 'no-store', signal: AbortSignal.timeout(8000) })
    if (response.ok && (await response.json()).releaseId === release.id) return { status: 'live', id: release.id }
  } catch { /* A network failure is not evidence that deployment failed. */ }
  const runs = await github.runs()
  const matching = runs.filter(run => run.display_title === `content-${release.id}`)
  if (matching.some(run => run.status !== 'completed')) return { status: 'publishing', id: release.id }
  // GitHub lists runs newest first: an older success must not hide a failed retry.
  if (matching[0]?.conclusion === 'success') return { status: 'verifying', id: release.id }
  if (matching.length) return { status: 'failed', id: release.id }
  return { status: 'not-started', id: release.id }
}
