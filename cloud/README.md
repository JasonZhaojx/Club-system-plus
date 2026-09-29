# Cloudflare + GitHub 内容发布与 Eventbrite PoC

本目录记录云端实现和配置。2026-09-26 用户已完成真实邮箱登录、私有草稿保存、首次 Pages 自动发布与草稿隔离验证；完整验收仍未完成。验收证据及剩余事项见 [项目状态](../context/project-status.md)。旧网站与 `legacy` 不受影响。

## 实现范围

- 公开网站：Cloudflare Pages 静态文件，只读取构建产物 `published.json`，不实时依赖 GitHub、后台或 Eventbrite。
- 后台：独立 Workers 地址，Cloudflare Access 邮箱验证码登录。Worker 对所有路径（包括页面资源）再次校验 JWT 的签名、issuer、audience、过期时间及受邀邮箱名单。配置不完整时拒绝访问，没有本地测试登录接口。
- 内容：独立私有 GitHub 仓库的 `content/state.json`。API 路径、仓库与分支由服务端固定；保存需要文件 SHA，拒绝过期版本和额外字段。
- 发布：冻结简介、发布 UUID 和当前 `main` 代码 SHA，再显式触发代码仓库的工作流。构建只导出选中字段，不复制私有内容仓库、其他草稿或身份名单。
- 自动部署：内容发布及 `main` 代码推送共用一个串行部署队列，上传前拒绝已过期的代码或内容版本。重复任务若版本已上线则跳过上传。代码推送只使用当时线上内容，不自动发布草稿。
- 状态：请求保存、构建开始与实际上线分开；后台每 15 秒查询。只有公开站点返回匹配发布 ID 才显示“已上线”。请求启动失败可重试相同发布；上线失败需根据 Actions 和 Pages 记录判断。

PoC 用独立 `*.workers.dev` 后台和 `*.pages.dev` 展示地址，不必先买域名。展示站 `/admin` 提供后台入口。正式同域 `/admin` 的路由整合留待域名配置，当前不是同域部署。

## 一次性配置顺序

### 1. 建立私有内容仓库

在 UNSWCSA 个人 GitHub 账号下创建私有内容仓库（例如 `UNSWCSA/unswcsa-content`）。用 `content-state.example.json` 初始化其 `main` 分支的 `content/state.json`。不要把真实草稿提交到本代码仓库；Worker 会拒绝使用公开内容仓库。

### 2. 配置 GitHub App

App 只安装到代码与内容这两个仓库。所需仓库权限为 Contents read/write、Actions read/write，Metadata 为 GitHub 必需权限；无需 Workflows 写权限。服务端申请短期 token 时按当前操作进一步限制仓库与权限。

记录 App ID、installation ID。私钥仅放 Worker Secret 和 GitHub Actions Secret，不能发到聊天或提交 Git。GitHub 下载的 RSA 私钥若是 `BEGIN RSA PRIVATE KEY`（PKCS#1），先在本机转换为本实现使用的 PKCS#8：

```sh
openssl pkcs8 -topk8 -nocrypt -in /path/to/downloaded-key.pem -out /path/to/app-pkcs8.pem
```

输入输出都在受控本机位置，不把密钥内容打印到终端。App 创建和安装由有权限管理该个人账号及目标仓库的人执行。

### 3. 建立 Cloudflare 项目和 Access

1. 建立 Pages **Direct Upload** 项目，生产分支为 `main`，记下 `https://项目名.pages.dev`。不要再开启另一套 Git 集成发布，以免绕过串行部署队列。
2. 构建并部署后台 Worker。第一次尚未填写安全配置时，所有访问返回 503，这是预期行为。
3. 在后台 Worker 的 `workers.dev` 地址启用 Cloudflare Access，保护整个主机名；身份提供方式启用 One-time PIN，Allow 规则只列受邀邮箱，不允许 Everyone，不设置绕过规则。
4. 记录 Access team 地址作为 issuer（如 `https://团队名.cloudflareaccess.com`，无末尾斜杠）及此应用的 AUD。
5. 在 Worker 设置中填下表变量和 Secrets。所有后台页面、接口及备用地址必须受到校验；preview URLs 默认关闭。

| Worker 变量 | 值 |
| --- | --- |
| `ACCESS_ISSUER` / `ACCESS_AUD` | Access team URL / 应用 AUD |
| `ADMIN_ORIGIN` | 后台 `https://...workers.dev`，无末尾斜杠 |
| `PUBLIC_ORIGIN` | 展示站 `https://...pages.dev`，无末尾斜杠 |
| `CONTENT_REPO` / `CONTENT_BRANCH` | 私有内容仓库 `账号或组织/仓库名` / `main` |
| `CODE_REPO` / `CODE_BRANCH` | `UNSWCSA/unswcsa-web` / `main` |
| `GITHUB_APP_ID` / `GITHUB_INSTALLATION_ID` | 上一步记录的 ID |

Worker Secrets：`GITHUB_PRIVATE_KEY`（PKCS#8 PEM）和 `ALLOWED_EMAILS`（逗号分隔的受邀邮箱）。人工撤权同时更新 Access 与名单；服务端逐次检查名单，可拒绝尚未过期的旧会话。

后台构建时仅提供公开 URL，不得给任何 `VITE_*` 变量放密钥。在 `frontend` 执行：

```sh
VITE_PUBLIC_ORIGIN=https://YOUR-PROJECT.pages.dev npm run build:cloud-admin
```

在 `cloud` 执行：

```sh
npm ci
npx wrangler login
npm run check
npm run deploy
```

`wrangler.toml` 的 `keep_vars` 保留 Dashboard 配置；也可复制为已忽略的 `wrangler.local.toml`，填非敏感变量，再用 `npx wrangler deploy --config wrangler.local.toml`。Secrets 始终通过 Dashboard 或 `wrangler secret put` 设置，不写 TOML。

### 4. 配置代码仓库 Actions

先把工作流提交到 `main`。在代码仓库 Settings → Secrets and variables → Actions 设置：

| Repository variable | 值 |
| --- | --- |
| `CONTENT_REPO` / `CONTENT_BRANCH` | 私有内容仓库 / `main` |
| `CONTENT_APP_ID` / `CONTENT_APP_INSTALLATION_ID` | GitHub App ID / installation ID |
| `PUBLIC_ORIGIN` / `ADMIN_ORIGIN` | 两个 HTTPS 地址，无末尾斜杠 |
| `CLOUDFLARE_ACCOUNT_ID` / `PAGES_PROJECT` | Cloudflare account ID / Pages 项目名称 |
| `CSA_PUBLISH_ENABLED` | 配好后才设为 `true`；缺失或其他值时部署作业跳过 |

Repository Secrets：`CONTENT_APP_PRIVATE_KEY`（同一 PKCS#8 私钥）、`CLOUDFLARE_PAGES_TOKEN`（仅目标 Cloudflare 账号所需 Pages 编辑权限）。使用标准 Linux runner；未配置这些值时不要将跳过的作业视为已通过。

第一次从后台保存测试简介并发布，创建首个公开版本。此后 `main` 的前端/云端代码修改自动构建，使用当前已上线内容。仅将已准备发布的代码提交到 `main`；发布请求后若 `main` 又有新提交，旧请求会被拒绝，需要重新发布。无需另开业务开发分支。

## 本地可验证项

```sh
# frontend
npm run build:cloud-admin
npm run build:cloud-public
# cloud
npm test
npm run check
```

`check` 是 dry-run，不发布。自动测试使用模拟 GitHub 响应和本地签名 JWT，验证逻辑但不代表真实服务联调。公开构建单独运行不会生成内容，需要发布工作流写入 `published.json`；不要直接拿空构建当作已完成网站部署。

## 真实验收清单（逐项记录，不代表全部通过）

已确认：允许邮箱登录、私有仓库保存、首次发布成功、后台确认已上线，以及后续只保存草稿不改变公开内容。首次工作流约 42 秒。尚无第二次发布、云端拒绝访问、冲突、失败恢复及回滚的完整证据。2026-09-28 本地已实现同内容和代码版本复用、持久化派发资格及重试前状态检查，用户已部署并提供重复点击后一个新增任务成功的截图；完整并发与失败恢复验收仍待补齐。网络响应丢失时仍存在重复任务的极端可能，不能宣称跨 GitHub 提交与派发的严格 exactly-once。


- 允许邮箱收到验证码并登录，其他邮箱/伪造或失效 JWT 被拒绝；直接请求草稿和资源不能绕过 Access。
- 保存草稿产生私有仓库提交，公开页面仍旧；无权限、分支规则拒绝及过期 SHA 有明确失败结果。
- 发布触发 Actions，成功后 Pages 返回新 release ID，后台显示已上线；GitHub/Access 不可用时公开站仍可访问。
- 人工制造构建失败，旧部署继续可用；修复后重试。连续发布、重复重试及代码同时更新不能回退到旧版本。
- 技术部在 Pages 恢复上一可用部署，并据此重新整理发布状态。当前不建设后台历史版本管理。
- 记录发布耗时、实际免费额度和 Worker CPU 使用；若签名/校验超过免费执行限制，先优化或调整免费方案，不自动启用付费。

## Eventbrite 与图片

此工作流不调用 Eventbrite；活动不进入内容 JSON 或全站构建依赖。2026-09-29 已通过独立 Eventbrite Worker 的本地真实读取和云端活动展示；完整故障恢复及与 React 官网整合仍待验收。启动、配置、部署和接口范围见 [Eventbrite PoC](events/README.md)。

本轮不启用 R2、不上传图片、不切换正式域名或关闭旧服务。新依赖安装审计为零漏洞；原前端依赖的既有漏洞仍需在对外上线前评估。

## 官方参考

- [Access JWT 校验](https://developers.cloudflare.com/cloudflare-one/access-controls/applications/http-apps/authorization-cookie/validating-json/)
- [保护 workers.dev 地址](https://developers.cloudflare.com/workers/configuration/routing/workers-dev/)
- [GitHub App installation token](https://docs.github.com/en/apps/creating-github-apps/authenticating-with-a-github-app/generating-an-installation-access-token-for-a-github-app)
- [GitHub 文件更新接口](https://docs.github.com/en/rest/repos/contents)
- [显式触发 Actions 工作流](https://docs.github.com/en/rest/actions/workflows)
- [Pages Direct Upload](https://developers.cloudflare.com/pages/get-started/direct-upload/)

## 2026-09-28 去重与故障恢复实现

- 相同草稿内容和代码 SHA 复用 release；排队、运行或已确认上线时不再次派发。失败版本使用明确的“重试发布”，不通过重复点击发布创建新版本。
- 派发前以内容文件 SHA 条件写入 `dispatchRequestedAt` 和 `dispatchAttempt`，跨 Worker 实例争用时只有一个写入成功；失败的旧请求返回 409。
- 提交后的两分钟内复用现有请求，以覆盖 Actions 任务可见性的延迟。超过窗口后检查线上版本和任务；状态查询失败不派发。代码改变后拒绝重试旧 release，应重新发布。
- GitHub 文件提交与 workflow dispatch 不是原子事务。响应丢失或任务超过两分钟仍不可见时，显式重试可能重复创建任务；已有构建串行队列和相同线上版本跳过上传保护仍保留。
- 本地新增六项模拟测试：并发发布、排队与上线复用、构建失败后并发重试和恢复、派发响应丢失／状态接口故障、保存失败与代码版本变化、新内容发布及较新失败不被旧成功掩盖。共 16 项云端逻辑测试通过，前后台类型检查与构建通过。
- 这里的构建失败和恢复是模拟 GitHub 任务状态与公开版本响应，未真正破坏云端构建、未执行 Pages 回滚，也不证明真实平台故障时的所有行为。

### 部署后验收

1. 推送代码到 main；后台前端以正确 PUBLIC_ORIGIN 构建后，单独部署 Worker（Actions 仅部署公开站）：
   ```sh
   # frontend 目录
   VITE_PUBLIC_ORIGIN=https://unswcsa-web-poc.pages.dev npm run build:cloud-admin
   # cloud 目录
   npm run deploy
   ```
2. 发布后重复点击同版本发布或重试，检查 Actions 不增加任务；首次派发后的两分钟内应复用请求。
3. 双标签页使用同一草稿版本同时操作，一份写入成功，另一份拒绝过期版本。
4. 云端构建失败与 Pages 上一版恢复仍需在 PoC 项目单独演练并记录。不要删除真实凭据制造故障，不修改旧官网。
