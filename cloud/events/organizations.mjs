import { pages } from './source.mjs'
let token = ''
for await (const chunk of process.stdin) token += chunk
try {
  const organizations = await pages('/users/me/organizations/', 'organizations', token.trim())
  console.log(JSON.stringify(organizations.map(({ id, name }) => ({ id, name })), null, 2))
} catch { console.error('无法读取组织，请检查 Token、权限或网络；未输出凭据。'); process.exitCode = 1 }
