import test from 'node:test'
import assert from 'node:assert/strict'
import { loadEvents, normalize, MESSAGE } from '../events/source.mjs'
import { createWorker } from '../events/worker.mjs'
const env = { EVENTBRITE_PRIVATE_TOKEN: 'test-secret', EVENTBRITE_ORGANIZATION_ID: '123' }
const event = { id: '42', organization_id: '123', listed: true, status: 'live', name: {text:'活动'}, start:{utc:'2026-10-01T00:00:00Z'}, end:{utc:'2026-10-01T02:00:00Z'}, url:'https://www.eventbrite.com/e/42', secret:'never expose' }
const response = (events, more = false, continuation) => Response.json({events, pagination:{has_more_items:more, continuation}})
const req = path => new Request('https://events.test'+path)
test('Eventbrite excludes private, draft, other organization and series parent records; uses public allowlist', () => {
  for (const change of [{listed:false},{invite_only:true},{status:'draft'},{organization_id:'999'},{is_series_parent:true}]) assert.equal(normalize({...event,...change}, '123'),null)
  assert.equal(normalize(event,'123').secret,undefined)
  assert.throws(() => normalize({...event,url:'javascript:alert(1)'},'123'))
})
test('Eventbrite pagination uses bearer auth, deduplicates and never returns partial results', async () => {
  let calls=0
  const items=await loadEvents(env,async (url,options) => { assert.equal(options.headers.Authorization,'Bearer test-secret'); calls++; if(calls===2) assert.match(url,/continuation=next/); return response([event],calls===1,'next') })
  assert.equal(items.length,1); assert.equal(calls,2)
  calls=0
  await assert.rejects(loadEvents(env,async () => ++calls===1 ? response([event],true,'next') : new Response('',{status:429})))
  await assert.rejects(loadEvents(env,async () => response([event],true,'same')))
})
test('Eventbrite failure replaces expired data, returns generic error, and recovers without redeploy',async () => {
  let clock=0, broken=false, calls=0
  const worker=createWorker(async()=>{calls++;return broken?new Response('',{status:401}):response([event])},()=>clock)
  assert.equal((await worker.fetch(req('/api/events'),env)).status,200)
  broken=true; clock=31000
  const failed=await worker.fetch(req('/api/events/42'),env)
  assert.equal(failed.status,503); assert.deepEqual(await failed.json(),{error:MESSAGE})
  broken=false; clock=42000
  assert.equal((await worker.fetch(req('/api/events/42'),env)).status,200)
  assert.equal((await worker.fetch(req('/api/events/99'),env)).status,404)
  assert.equal(calls,3)
})
test('Eventbrite concurrent reads coalesce; empty success differs from missing configuration',async()=>{
  let calls=0
  const worker=createWorker(async()=>{calls++; await new Promise(resolve=>setTimeout(resolve,10)); return response([])})
  const responses=await Promise.all([worker.fetch(req('/api/events'),env),worker.fetch(req('/api/events'),env)])
  assert.equal(calls,1); assert.deepEqual(await responses[0].json(),{events:[]})
  assert.equal((await worker.fetch(req('/api/events'),{})).status,503)
})
