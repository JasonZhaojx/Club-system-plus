# Update List

用于记录当前系统已经识别、但暂未完成的升级点和修改项。后续开发时可以按优先级逐步处理。

## 认证与鉴权

### JWT 安全增强

当前系统使用 JWT 做前后端分离 API 鉴权，适合当前 React + Spring Boot 的架构。但正式部署前需要补充以下安全能力：

- 将 JWT secret 从 `application.yml` 移到环境变量或密钥管理服务。
- 生产环境强制使用 HTTPS。
- 将 access token 有效期缩短到 15-30 分钟。
- 引入 refresh token 机制。
- refresh token 推荐存放在 `HttpOnly + Secure + SameSite` Cookie 中。
- 修改密码后让旧 token 失效。
- 用户被禁用或删除后，已有 token 必须失效。
- 增加 token version 字段，JWT 中携带版本号，后端每次校验时比对数据库版本。
- 如需主动踢下线，可增加 Redis token 黑名单或在线 token 表。

### localStorage 存储风险

当前前端将 access token 存在 `localStorage`，实现简单，但存在 XSS 盗取 token 的风险。

后续建议：

- access token 可继续短期存储，但必须缩短有效期。
- refresh token 不放入 `localStorage`。
- 前端输入内容统一转义，减少 XSS 风险。
- 后台富文本、活动描述、RAG 回答渲染时必须做 HTML 清洗。

### Session / Cookie 方案评估

当前没有使用传统 Session，原因是项目是前后端分离 API 架构，JWT 对多端和多服务更方便。

但 Cookie 仍然可以参与安全升级：

- 不建议使用传统单机 Session。
- 如果后续要使用 Session，应配合 Redis Session，避免多实例部署时会话丢失。
- 推荐方案是 `JWT access token + HttpOnly Cookie refresh token`。

## 登录与账号安全

- 增加登录失败次数限制。
- 增加账号临时锁定策略。
- 增加图形验证码或邮箱验证码，用于防暴力破解。
- root 初始密码首次登录后强制修改。
- 修改密码后重新登录或刷新 token。
- 增加登录日志，包括登录时间、IP、User-Agent、成功/失败状态。
- 增加用户敏感操作日志，如修改密码、修改邮箱、角色变更。

## 权限系统

- 当前 RBAC 已支持角色和权限码，后续需要补充权限管理页面。
- 用户角色调整需要做操作审计。
- 部门负责人权限需要增加部门范围约束，不能只依赖全局权限码。
- 后端接口需要继续补充 `@PreAuthorize`，确保前端隐藏按钮不是唯一防线。
- 前端菜单和按钮权限应统一封装，避免每个页面重复判断。

## 个人资料

- 邮箱修改后建议增加邮箱验证码确认。
- 修改密码可以增加密码强度提示。
- 用户资料页后续可增加头像上传。
- 部门和加入时间目前依赖 `club_member`，后续成员管理模块完成后需要统一维护。

## 前端交互

- 全局 toast 已支持 5 秒自动消失和手动关闭，后续可增加队列，避免多个错误互相覆盖。
- 表单错误和全局错误需要保持边界：登录、注册、修改密码等留在表单底部，普通接口错误走全局 toast。
- 弹窗需要后续补充 ESC 关闭、点击遮罩关闭和焦点管理。

## 数据库与初始化

- 当前 `schema.sql` 使用 `insert ignore` 初始化角色、权限和 root 用户，适合开发阶段。
- 后续建议引入 Flyway 或 Liquibase 管理数据库迁移。
- root 初始密码不应长期写死在 SQL 中。
- 已有数据环境升级时，需要避免初始化脚本覆盖或污染生产数据。
