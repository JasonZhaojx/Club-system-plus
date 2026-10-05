# 新版官网独立入口

在 `frontend` 目录运行：

- `npm run dev:site`：本地开发，http://127.0.0.1:5174。
- `npm run build:site`：独立类型检查及构建，输出到 `frontend/dist-site-preview/`。
- `npm run preview:site`：构建后预览，http://127.0.0.1:4174。

本入口使用独立 Router、样式及 TypeScript 配置，共用已有 React/Vite 依赖与 `public/brand/` 正式资产，不导入旧平台的认证、API 拦截器或全局样式。目前只有入口确认页面与兜底 404；六页设计和真实数据接入尚未实现。预览 HTML 带有 noindex，正式发布时需重新确认索引策略。

默认 dev/build、旧 src、本地 poc 和云端 cloud 入口保留。现有发布工作流排除仅涉及 `frontend/site/**` 和 `frontend/vite.site.config.mjs` 的 push；共享 package.json、品牌素材、cloud 代码及发布工作流的变化仍可触发 PoC 发布。手动内容发布仍可用。

2026-10-05：新版及云端前后台构建通过。独立入口未配置云端部署，没有切换正式域名。
