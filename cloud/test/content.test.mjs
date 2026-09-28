import { test } from 'node:test'
import assert from 'node:assert/strict'
import { changeContent, releaseStatus } from '../src/content.mjs'
import { publicPayload } from '../src/public.mjs'
import { decodeJson, encodeJson, githubClient, HttpError } from '../src/github.mjs'
import { generateKeyPair, exportPKCS8 } from 'jose'

const sha = 'a'.repeat(40)
function fakeGithub() {
  let version = 10
  let value = { revision: sha, state: { draft: { introduction: '初始草稿' }, release: null } }
  return {
    read: async () => structuredClone(value),
    write: async (revision, state) => {
      if (revision !== value.revision) throw new HttpError(409, 'conflict')
      value = { revision: (++version).toString(16).padStart(40, '0'), state: structuredClone(state) }; return structuredClone(value)
    },
    codeHead: async () => sha,
    dispatch: async () => {},
    runs: async () => [],
  }
}
test('draft save never changes release, and stale writes are rejected', async () => {
  const github = fakeGithub()
  const saved = await changeContent(github, 'draft', { revision: sha, introduction: '新草稿' })
  assert.equal(saved.state.release, null)
  assert.equal(saved.state.draft.introduction, '新草稿')
  await assert.rejects(changeContent(github, 'draft', { revision: sha, introduction: '旧浏览器' }), { status: 409 })
})
test('publish freezes content and code; dispatch failure remains retryable', async () => {
  const github = fakeGithub()
  github.dispatch = async () => { throw new Error('network') }
  const saved = await changeContent(github, 'publish', { revision: sha })
  assert.equal(saved.dispatchFailed, true)
  assert.equal(saved.state.release.codeSha, sha)
  assert.equal(saved.state.release.content.introduction, '初始草稿')
  let dispatched
  github.dispatch = async id => { dispatched = id }
  const retried = await changeContent(github, 'retry', { revision: saved.revision }, { now: () => Date.now() + 121000 })
  assert.equal(dispatched, saved.state.release.id)
  assert.notEqual(retried.revision, saved.revision)
})
test('illegal fields, missing versions and oversized values are rejected', async () => {
  for (const input of [{ revision: sha, introduction: 'x', path: '.github/workflows/evil.yml' }, { introduction: 'x' }, { revision: sha, introduction: 'x'.repeat(601) }, { revision: sha, introduction: '' }]) {
    await assert.rejects(changeContent(fakeGithub(), 'draft', input), { status: 400 })
  }
})
test('GitHub Unicode encoding round-trips without corruption', () => {
  const content = { introduction: '新南学联 🌏\n第二段' }
  assert.deepEqual(decodeJson(encodeJson(content)), content)
})
test('public artifact contains only explicitly selected fields', () => {
  const input = { releaseId: crypto.randomUUID(), content: { introduction: '公开', privateNote: 'secret' }, publishedAt: new Date().toISOString(), draft: { introduction: 'secret' }, email: 'secret' }
  const result = publicPayload(input, sha)
  assert.ok(!JSON.stringify(result).includes('secret'))
  assert.deepEqual(Object.keys(result), ['releaseId', 'content', 'publishedAt', 'codeSha'])
  assert.throws(() => publicPayload({ ...input, content: { introduction: '' } }, sha))
})
test('only matching online manifest means live; build success alone is verifying', async () => {
  const github = fakeGithub(); const release = { id: crypto.randomUUID() }
  const manifest = id => async () => Response.json({ releaseId: id })
  assert.equal((await releaseStatus(github, release, 'https://site.example', manifest(release.id))).status, 'live')
  github.runs = async () => [{ display_title: `content-${release.id}`, status: 'completed', conclusion: 'success' }]
  assert.equal((await releaseStatus(github, release, 'https://site.example', manifest('old'))).status, 'verifying')
  github.runs = async () => [{ display_title: `content-${release.id}`, status: 'completed', conclusion: 'failure' }]
  assert.equal((await releaseStatus(github, release, 'https://site.example', manifest('old'))).status, 'failed')
})
test('GitHub adapter refuses public content repo and narrows installation token', async () => {
  const { privateKey } = await generateKeyPair('RS256', { extractable: true })
  const env = { GITHUB_PRIVATE_KEY: await exportPKCS8(privateKey), GITHUB_APP_ID: '123', GITHUB_INSTALLATION_ID: '456', CONTENT_REPO: 'org/content', CONTENT_BRANCH: 'main' }
  const calls = []
  const client = githubClient(env, async (url, options) => {
    calls.push([url, options])
    if (url.endsWith('access_tokens')) return Response.json({ token: 'test-token' })
    return Response.json({ private: false })
  })
  await assert.rejects(client.read(), { status: 503 })
  assert.deepEqual(JSON.parse(calls[0][1].body), { repositories: ['content'], permissions: { contents: 'read' } })
  assert.equal(calls.length, 2)
})

test('simultaneous publication uses one durable claim; repeated requests reuse it', async () => {
  const github = fakeGithub(); let calls = 0
  github.dispatch = async () => { calls++ }
  const results = await Promise.allSettled([
    changeContent(github, 'publish', { revision: sha }),
    changeContent(github, 'publish', { revision: sha }),
  ])
  assert.equal(results.filter(result => result.status === 'fulfilled').length, 1)
  assert.equal(results.find(result => result.status === 'rejected').reason.status, 409)
  const current = await github.read()
  for (const action of ['publish', 'retry', 'publish']) {
    const result = await changeContent(github, action, { revision: current.revision })
    assert.equal(result.reused, true)
    assert.equal(result.state.release.id, current.state.release.id)
  }
  assert.equal(calls, 1)
})

test('visible queued builds and confirmed live versions suppress another dispatch', async () => {
  const github = fakeGithub(); let calls = 0
  github.dispatch = async () => { calls++ }
  const current = await changeContent(github, 'publish', { revision: sha })
  const options = { now: () => Date.now() + 121000, publicOrigin: 'https://site.example', fetcher: async () => Response.json({ releaseId: 'previous' }) }
  github.runs = async () => [{ display_title: `content-${current.state.release.id}`, status: 'queued' }]
  assert.equal((await changeContent(github, 'retry', { revision: current.revision }, options)).reused, true)
  options.fetcher = async () => Response.json({ releaseId: current.state.release.id })
  assert.equal((await changeContent(github, 'publish', { revision: current.revision }, options)).reused, true)
  assert.equal(calls, 1)
})

test('failed build preserves public content and a concurrent retry dispatches once, then recovers', async () => {
  const github = fakeGithub(); let calls = 0
  let publicManifest = { releaseId: 'previous', content: { introduction: 'previous public content' } }
  github.dispatch = async () => { calls++ }
  const first = await changeContent(github, 'publish', { revision: sha })
  const options = { now: () => Date.now() + 121000, publicOrigin: 'https://site.example', fetcher: async () => Response.json(publicManifest) }
  github.runs = async () => [{ display_title: `content-${first.state.release.id}`, status: 'completed', conclusion: 'failure' }]
  assert.equal((await releaseStatus(github, first.state.release, options.publicOrigin, options.fetcher)).status, 'failed')
  const repeat = await changeContent(github, 'publish', { revision: first.revision }, options)
  assert.equal(repeat.retryRequired, true)
  assert.equal(calls, 1)
  const results = await Promise.allSettled([1, 2].map(() => changeContent(github, 'retry', { revision: first.revision }, options)))
  assert.equal(results.filter(result => result.status === 'fulfilled').length, 1)
  assert.equal(calls, 2)
  assert.equal(publicManifest.content.introduction, 'previous public content')
  const current = await github.read()
  assert.equal(current.state.release.id, first.state.release.id)
  // Simulate successful deployment propagation after recovery.
  publicManifest = { releaseId: first.state.release.id, content: first.state.release.content }
  assert.equal((await releaseStatus(github, current.state.release, options.publicOrigin, options.fetcher)).status, 'live')
})

test('lost dispatch response waits for visibility; status outage never starts another build', async () => {
  const github = fakeGithub(); let calls = 0
  github.dispatch = async () => { calls++; throw new Error('response lost after acceptance') }
  const first = await changeContent(github, 'publish', { revision: sha })
  assert.equal(first.dispatchFailed, true)
  assert.equal((await changeContent(github, 'retry', { revision: first.revision })).reused, true)
  github.runs = async () => { throw new Error('GitHub unavailable') }
  await assert.rejects(changeContent(github, 'retry', { revision: first.revision }, { now: () => Date.now() + 121000, fetcher: async () => { throw new Error('site unavailable') } }))
  assert.equal(calls, 1)
  assert.equal((await github.read()).revision, first.revision)
})

test('failed save cannot dispatch, and changed code requires a new publication', async () => {
  const github = fakeGithub(); let calls = 0
  github.dispatch = async () => { calls++ }
  const write = github.write
  github.write = async () => { throw new Error('write unavailable') }
  await assert.rejects(changeContent(github, 'publish', { revision: sha }))
  assert.equal(calls, 0)
  github.write = write
  const first = await changeContent(github, 'publish', { revision: sha })
  github.codeHead = async () => 'c'.repeat(40)
  await assert.rejects(changeContent(github, 'retry', { revision: first.revision }), { status: 409 })
  const next = await changeContent(github, 'publish', { revision: first.revision })
  assert.notEqual(next.state.release.id, first.state.release.id)
  assert.equal(next.state.release.codeSha, 'c'.repeat(40))
  assert.equal(calls, 2)
})

test('new content creates a new release, and a failed retry is not hidden by an older success', async () => {
  const github = fakeGithub()
  const first = await changeContent(github, 'publish', { revision: sha })
  const draft = await changeContent(github, 'draft', { revision: first.revision, introduction: '第二版' })
  const next = await changeContent(github, 'publish', { revision: draft.revision })
  assert.notEqual(next.state.release.id, first.state.release.id)
  assert.equal(next.state.release.content.introduction, '第二版')
  github.runs = async () => ['failure', 'success'].map(conclusion => ({ display_title: `content-${next.state.release.id}`, status: 'completed', conclusion }))
  assert.equal((await releaseStatus(github, next.state.release, 'https://site.example', async () => Response.json({ releaseId: 'old' }))).status, 'failed')
})
