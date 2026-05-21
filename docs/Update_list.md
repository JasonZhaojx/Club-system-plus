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

## 优惠券秒杀队列与 Redis 存储决策

### 当前状态

优惠券领取已经引入 Redis 做高并发前置校验，但活动报名目前仍然走 MySQL 事务和条件更新。

当前优惠券 Redis key：

- `coupon:batch:{batchId}:stock`：String，保存 Redis 侧剩余库存。
- `coupon:batch:{batchId}:users`：Set，保存已成功抢券的用户 ID，用于防重复领取。
- `coupon:claim:queue`：List，保存 `batchId:userId`，由后端定时任务异步消费并写入 MySQL。

当前这些 key 没有设置 TTL，长期运行后会产生历史批次数据堆积。后续需要增加过期策略，并考虑用 RabbitMQ 替代 Redis List 承担异步任务队列职责。

### 方案权衡

#### 方案一：只使用 MySQL

流程为请求直接访问数据库，通过唯一索引和条件更新控制重复领取与库存。

优点：

- 实现简单。
- 数据一致性强。
- 不依赖额外中间件。

缺点：

- 高并发下大量请求直接打到 MySQL。
- 热门批次会形成行锁竞争。
- 不适合秒杀、抢票这类瞬时流量场景。

适用范围：

- 普通报名、普通领取。
- 并发量较低的后台管理功能。

#### 方案二：Redis Lua + Redis List

流程为 Redis Lua 原子判断库存和重复领取，成功后写入 Redis List，由后端定时任务消费并落库。

优点：

- Redis Lua 可以保证库存扣减和重复判断的原子性。
- 能显著减少 MySQL 压力。
- 实现成本比引入消息队列低。

缺点：

- Redis List 的消息确认、失败重试、死信处理能力较弱。
- 服务异常时，队列消息处理状态不够清晰。
- 需要额外补偿逻辑处理 Redis 成功但数据库未落库的情况。

适用范围：

- 当前开发阶段。
- 中等并发压测。
- 对消息可靠性要求还没有完全生产化的场景。

#### 方案三：Redis Lua + RabbitMQ

流程为 Redis Lua 负责原子扣库存和防重复，成功请求写入本地任务表并投递 RabbitMQ，由消费者异步写 MySQL。

优点：

- Redis 负责抗高并发，RabbitMQ 负责削峰和可靠异步处理。
- RabbitMQ 支持 ACK、NACK、重试和死信队列。
- MySQL 可以通过唯一索引和条件更新做最终兜底。
- 领取接口可以快速返回，落库异步完成。

缺点：

- 系统复杂度上升。
- 需要维护 RabbitMQ 连接、交换机、队列、死信队列和消费者。
- 需要处理 Redis 成功但 RabbitMQ 投递失败的补偿。

适用范围：

- 优惠券限量领取。
- 活动抢票。
- 高并发秒杀类接口。

推荐后续采用该方案。

#### 方案四：Redis Lua + Kafka

Kafka 也可以承接异步事件，但当前业务不是最优选择。

优点：

- 高吞吐。
- 适合事件流、日志流、行为轨迹和多消费者订阅。
- 消息可长期按 topic 保存。

缺点：

- 对单条业务任务的 ACK、失败重试和死信处理不如 RabbitMQ 直接。
- 运维和概念复杂度更高。
- 当前系统主要需要“领取任务可靠落库”，不是大规模事件流分析。

结论：

- Kafka 暂不作为优惠券和抢票队列首选。
- 如果后续增加行为日志、推荐系统、数据分析链路，可以再引入 Kafka。

### 推荐架构

优惠券和后续活动抢票建议统一采用：

```text
用户请求
  -> Spring Boot 基础校验
  -> Redis Lua 原子校验库存和重复领取
  -> 写入本地任务表 coupon_claim_task / activity_ticket_task
  -> 投递 RabbitMQ
  -> 消费者异步写 MySQL
  -> 成功后任务标记 DONE
  -> 失败进入重试或死信队列
```

Redis 只保存高并发判断所需的临时状态：

- `coupon:batch:{batchId}:stock`
- `coupon:batch:{batchId}:users`
- `activity:{activityId}:stock`
- `activity:{activityId}:users`

RabbitMQ 保存异步落库任务：

- Exchange：`coupon.claim.exchange`
- Queue：`coupon.claim.queue`
- Dead Letter Queue：`coupon.claim.dlq`
- 后续活动抢票可增加 `activity.ticket.exchange`、`activity.ticket.queue`、`activity.ticket.dlq`

MySQL 继续作为最终事实来源：

- `coupon_batch.claimed_count`
- `user_coupon`
- `coupon_claim_task`
- `activity.registered_count`
- `activity_registration`
- 后续可增加 `activity_ticket_task`

### Redis TTL 策略

为避免 Redis 历史数据堆积，需要给批次级 key 设置 TTL。

优惠券：

```text
TTL = 优惠券过期时间 expire_at - 当前时间 + 1 天缓冲
```

活动：

```text
TTL = 活动结束时间 end_time - 当前时间 + 1 天缓冲
```

需要设置 TTL 的 key：

- `coupon:batch:{batchId}:stock`
- `coupon:batch:{batchId}:users`
- `activity:{activityId}:stock`
- `activity:{activityId}:users`

不建议给 RabbitMQ 替代后的业务队列使用 Redis TTL，因为队列职责应交给 RabbitMQ。

如果优惠券或活动被管理员提前结束，可以主动删除对应 Redis key：

```text
DEL coupon:batch:{batchId}:stock
DEL coupon:batch:{batchId}:users
DEL activity:{activityId}:stock
DEL activity:{activityId}:users
```

### 一致性与补偿

需要重点处理 Redis 成功但消息没有最终落库的问题。

推荐机制：

- Redis Lua 成功后，创建本地任务表记录，状态为 `PENDING`。
- RabbitMQ 投递成功后，由消费者处理任务。
- 消费者写库成功后，将任务标记为 `DONE`。
- 消费失败时，依赖 RabbitMQ 重试和死信队列。
- 定时补偿任务扫描 `PENDING` / `FAILED` 任务，重新投递 RabbitMQ。
- MySQL 保留唯一索引，防止重复消息导致重复领券或重复报名。
- MySQL 库存更新必须使用条件更新，防止最终层面超发。

MySQL 库存兜底 SQL 应保持类似形式：

```sql
UPDATE coupon_batch
SET claimed_count = claimed_count + 1
WHERE id = ?
  AND claimed_count < stock;
```

活动报名也应保持类似形式：

```sql
UPDATE activity
SET registered_count = registered_count + 1
WHERE id = ?
  AND registered_count < capacity;
```

### 实施顺序

建议按以下顺序升级：

1. 给现有优惠券 Redis key 增加 TTL，解决历史 key 堆积。
2. 引入 RabbitMQ 依赖和基础配置。
3. 新增优惠券领取交换机、队列、死信队列。
4. 将 `coupon:claim:queue` Redis List 替换为 RabbitMQ 消息。
5. 保留并强化 `coupon_claim_task` 作为本地补偿表。
6. 增加 RabbitMQ 消费者，异步写入 `user_coupon`。
7. 增加补偿任务，扫描未完成任务并重新投递。
8. 将同一套模式扩展到活动抢票接口。
## 2026-05-18 性能优化更新

### Dashboard overview 加载优化

已调整 overview 数据加载方式：

- overview 接口不再在请求线程里同步执行 `refreshApiAccessMinuteStats`、`refreshApiPathHourStats`、`refreshUserActivityDayStats`。
- 汇总表刷新继续交给 `DashboardStatScheduler` 后台定时任务执行。
- Redis 缓存只缓存统计图表和排行数据，不再缓存最近 API 日志和操作日志。
- 最近 API 日志、操作日志每次请求实时查询最新 20 条，避免面板出现“日志不更新”的错觉。

原因：

- 原实现缓存未命中时会同步扫描 `api_access_log` 并回写多个汇总表，日志量变大后会直接拖慢 overview。
- 最近日志被包含在 45 秒 overview 缓存中，新增日志不会立刻反映到页面。

### 数据库索引优化

已在 `schema.sql` 和启动初始化器中补充复合索引，覆盖已有数据库和新建数据库两种场景。

新增索引重点覆盖：

- 活动公开列表：`activity(status, category, start_time, id)`
- 活动后台最新排序：`activity(status, published_at, id)`
- 活动容量排序/报名容量判断：`activity(status, registered_count, capacity)`
- 活动报名统计：`activity_registration(activity_id, status)`
- 部门成员筛选：`club_member(department_id, status)`
- 优惠券批次分页：`coupon_batch(status, created_at, id)`
- 优惠券领取窗口：`coupon_batch(status, claim_start_time, claim_end_time, expire_time)`
- 用户券包查询：`user_coupon(user_id, status)`
- 批次券状态统计：`user_coupon(batch_id, status)`

实现文件：

- `backend/src/main/resources/schema.sql`
- `backend/src/main/java/com/backend/sever/config/DatabaseIndexInitializer.java`

### Redis 热点数据缓存

已对稳定且高频的读接口增加 Redis 缓存：

- 公开活动列表：`hot:activity:list:*`，TTL 30 秒
- 公开活动详情：`hot:activity:detail:{activityId}`，TTL 60 秒
- 部门列表：`hot:department:list`，TTL 5 分钟

写操作会主动清理相关缓存：

- 创建/更新/提交/发布/取消/结束活动
- 活动报名/取消报名
- 创建/更新/启用/停用部门

Redis 不可用时自动降级为查数据库，不影响主流程。

实现文件：

- `backend/src/main/java/com/backend/sever/service/impl/ActivityServiceImpl.java`
- `backend/src/main/java/com/backend/sever/service/impl/OrganizationServiceImpl.java`

### 接口令牌桶限流

新增 Redis Lua 令牌桶限流过滤器，按 `IP + Method + 归一化路径` 维度限流。

默认配置：

```yaml
app:
  rate-limit:
    enabled: true
    capacity: 120
    refill-per-second: 60
```

行为：

- 每个桶容量 120
- 每秒补充 60 个 token
- 每个请求消耗 1 个 token
- 超限返回 HTTP `429`
- Redis 不可用时放行，避免限流组件影响业务可用性
- 路径中的数字 ID 会归一化为 `{id}`，避免 `/activities/1`、`/activities/2` 产生过多限流 key

实现文件：

- `backend/src/main/java/com/backend/sever/config/RateLimitProperties.java`
- `backend/src/main/java/com/backend/sever/config/TokenBucketRateLimitFilter.java`
- `backend/src/main/resources/application.yml`

## 2026-05-18 当前性能与缓存实现状态

### Dashboard 缓存与刷新

当前 `dashboard:overview:v1` 只缓存 overview 的统计、图表和排行数据：

- 基础指标：用户数、成员数、部门数、活动数、报名数、优惠券领取数。
- 图表数据：活动状态、成员状态、API 访问趋势。
- 排行数据：热门接口、慢接口、异常接口、活动转化率、优惠券领取转化率、活跃用户排行。

当前不再缓存最近 API 日志和操作日志：

- `apiLogs` 每次 overview 请求实时查询最新 20 条。
- `operationLogs` 每次 overview 请求实时查询最新 20 条。

刷新行为：

- 自动刷新和用户手动刷新目前都调用同一个 overview 接口。
- 如果 `dashboard:overview:v1` 命中，统计、图表和排行会直接走缓存。
- 如果 `dashboard:overview:v1` 未命中，后端查询当前汇总表并重新写入 Redis。
- 手动刷新当前不会强制删除 `dashboard:overview:v1`，因此统计类数据最多存在 45 秒缓存延迟。
- 不在用户请求线程中同步刷新汇总表，避免恢复之前 9-12 秒加载问题。

### Dashboard 定时任务

后台定时任务负责刷新汇总表和清理历史数据：

- 每 1 分钟刷新 `api_access_minute_stat` 最近 2 小时数据，并删除 `dashboard:overview:v1`。
- 每 10 分钟刷新 `api_path_hour_stat` 最近 48 小时数据。
- 每 10 分钟刷新 `user_activity_day_stat` 最近 7 天数据。
- 每天 03:20 执行日志和汇总表清理。

SQL 保留周期：

- `api_access_log`：保留 30 天。
- `operation_log`：保留 180 天。
- `api_access_minute_stat`：保留 14 天。
- `api_path_hour_stat`：保留 90 天。
- `user_activity_day_stat`：保留 180 天。

### Redis Key 当前策略

当前 Redis key 与 TTL：

- `dashboard:overview:v1`：45 秒；定时任务刷新汇总后会主动删除。
- `hot:activity:list:*`：30 秒。
- `hot:activity:detail:{activityId}`：60 秒。
- `hot:department:list`：5 分钟。
- `rate:bucket:{ip}:{method}:{path}`：120 秒。
- `coupon:batch:{batchId}:stock`：按优惠券 `expire_time + 5 天` 过期。
- `coupon:batch:{batchId}:users`：按优惠券 `expire_time + 5 天` 过期。

`coupon:claim:queue` 暂不设置短 TTL。它是待落库队列，应由 `CouponClaimWorker` 消费清空，不能因为 Redis 过期导致未落库任务丢失。

### 限流当前状态

当前已实现 Redis Lua 令牌桶限流：

- 维度：`IP + Method + 归一化路径`。
- 默认桶容量：120。
- 默认补充速率：60 token/s。
- 超限返回 HTTP `429`。
- Redis 异常时放行，避免限流组件影响核心业务可用性。

说明：

- 该限流适合保护接口入口。
- 压测接口真实 QPS 时，需要临时关闭或调高 `app.rate-limit`。
- 后续如需保护单个活动或单个优惠券批次，应增加热点参数限流，例如 `activityId`、`couponBatchId` 维度。

### 优惠券 Redis Key 过期策略

已给优惠券批次相关 Redis key 增加 TTL：

- `coupon:batch:{batchId}:stock`
- `coupon:batch:{batchId}:users`

过期时间规则：

- 优先按 `coupon_batch.expire_time + 5 天` 计算。
- 如果批次没有 `expire_time`，默认保留 5 天。
- 如果计算结果已经过期，则至少保留 60 秒，避免写入 Redis 时 TTL 非法。

说明：

- `stock` key 在优惠券预加载时写入 TTL。
- `users` key 可能在用户首次抢券时才创建，因此 Lua 抢券脚本每次会同步给 `stock/users` 设置 TTL。
- `coupon:claim:queue` 暂不设置短 TTL，因为它是待落库队列，应由 worker 消费清空，不能因为过期导致未落库任务丢失。

## 2026-05-19 Caffeine 本地缓存与二级缓存改造

### 本次改动范围

本次后端缓存改造分为两部分：

- 引入 Caffeine 本地缓存，用于降低高频权限查询和稳定元数据查询对 MySQL 的压力。
- 在公开活动模块加入 `Caffeine -> Redis -> MySQL` 二级缓存链路，并重点处理写操作后的缓存失效时机。

实现文件：

- `backend/pom.xml`
- `backend/src/main/java/com/backend/common/auth/AuthorizationCache.java`
- `backend/src/main/java/com/backend/common/auth/JwtAuthenticationFilter.java`
- `backend/src/main/java/com/backend/sever/service/impl/RbacServiceImpl.java`
- `backend/src/main/java/com/backend/sever/service/impl/OrganizationServiceImpl.java`
- `backend/src/main/java/com/backend/sever/service/impl/ActivityServiceImpl.java`

### 权限缓存：userId -> roles / permissions

原实现中，`JwtAuthenticationFilter` 每次请求解析 JWT 后都会通过 `RoleMapper` 和 `PermissionMapper` 查询数据库，获取当前用户角色和权限码。该逻辑位于所有受保护接口的入口链路上，请求频率高，但用户权限变化频率低，因此适合加入本地缓存。

当前实现：

- 新增 `AuthorizationCache`。
- 缓存 key：`userId`。
- 缓存 value：`AuthorizationSnapshot(roles, permissions)`。
- TTL：3 分钟。
- 最大容量：20000 个用户。
- `JwtAuthenticationFilter` 不再直接访问 mapper，而是通过 `AuthorizationCache.get(userId)` 获取权限快照。

策略选择：

- 选择 Caffeine，而不是 Redis，原因是鉴权链路发生在每个请求入口，本地缓存延迟更低，不需要额外网络往返。
- 当前项目主要是单体 Spring Boot 部署，本地缓存能直接解决重复查库问题。
- TTL 设置为 3 分钟，是在“权限变更及时性”和“减少数据库压力”之间取平衡。
- 如果后期多实例部署，可以继续保留 Caffeine，并通过 Redis Pub/Sub、MQ 或短 TTL 控制多实例缓存一致性。

### 权限缓存失效策略

权限缓存不能只依赖 TTL，否则管理员刚调整用户角色后，用户可能在 TTL 窗口内继续使用旧权限。因此本次在权限写路径上增加主动失效：

- `RbacServiceImpl.assignUserRoles`：用户角色被重新分配后，失效该用户的权限快照。
- `RbacServiceImpl.assignRolePermissions`：角色权限被重新分配后，失效所有用户权限快照。
- `OrganizationServiceImpl.assignMemberToDepartment`：用户加入部门并升级为社团成员后，失效该用户权限快照。
- `OrganizationServiceImpl.updateMemberStatus`：成员启用/停用导致角色变化后，失效该用户权限快照。
- `OrganizationServiceImpl.appointDepartmentLeader`：任命部门负责人后，失效该用户权限快照。
- `OrganizationServiceImpl.removeDepartmentLeader`：移除部门负责人后，失效该用户权限快照。

失效时机：

- 如果当前存在 Spring 事务，则注册 `TransactionSynchronization.afterCommit`，等事务提交成功后再清缓存。
- 如果当前没有事务，则立即清缓存。

这样做的原因：

- 如果在事务提交前删除缓存，并发请求可能立刻读取数据库旧值，然后重新写入 Caffeine，导致旧权限在缓存中继续存在。
- afterCommit 失效可以避免“先删缓存、事务未提交、旧数据回填缓存”的问题。
- 如果事务回滚，则不会误删缓存，避免无意义的缓存抖动。

### 角色列表与权限列表缓存

`RbacServiceImpl.listRoles` 和 `RbacServiceImpl.listPermissions` 也加入了 Caffeine：

- 角色列表缓存 key：`all`。
- 权限列表缓存 key：`all`。
- TTL：10 分钟。
- 最大容量：10。

策略选择：

- 当前系统没有开放创建/删除角色、权限定义的接口，角色和权限码属于低频变化的系统元数据。
- 因此这里采用短 TTL 自动刷新即可，不需要复杂主动失效。
- 用户角色绑定和角色权限绑定变化不会改变角色/权限定义本身，只会影响用户鉴权快照，因此只需要失效 `AuthorizationCache`。

### 公开活动二级缓存

公开活动列表和公开活动详情原本已有 Redis 热点缓存。本次在 Redis 前增加 Caffeine，形成二级缓存：

```text
Caffeine 本地缓存
  -> Redis 分布式缓存
  -> MySQL
```

当前缓存对象：

- 公开活动列表：`listPublicActivities(keyword, category, sort, page, size)`
- 公开活动详情：`getPublicActivity(activityId)`

缓存策略：

- 活动列表 L1 Caffeine TTL：20 秒。
- 活动列表 L2 Redis TTL：30 秒。
- 活动详情 L1 Caffeine TTL：45 秒。
- 活动详情 L2 Redis TTL：60 秒。
- Caffeine 命中后直接返回。
- Caffeine 未命中但 Redis 命中时，将 Redis 数据回填到 Caffeine。
- Redis 未命中时查询 MySQL，并同时写入 Redis 和 Caffeine。

策略选择：

- 活动公开页属于读多写少场景，适合缓存。
- 列表受筛选、排序、分页参数影响，key 数量可能较多，因此本地缓存 TTL 较短，最大容量限制为 1000。
- 详情页 key 更稳定，且访问重复度更高，因此本地 TTL 稍长，最大容量限制为 5000。
- Redis 继续作为跨实例共享缓存，Caffeine 作为单实例热点加速层。
- Caffeine TTL 略短于 Redis TTL，降低本地旧数据停留时间，同时允许 Redis 承接二级命中。

### 公开活动缓存失效策略

活动写操作后会主动清理活动公开缓存：

- 创建活动
- 更新活动
- 提交审核
- 发布活动
- 取消活动
- 结束活动
- 报名活动
- 取消报名

失效范围：

- 本地 Caffeine 活动列表：全部清理。
- 本地 Caffeine 活动详情：按 `activityId` 精确清理。
- Redis 活动列表：按 `hot:activity:list:*` 清理。
- Redis 活动详情：按 `hot:activity:detail:{activityId}` 精确清理。

失效时机：

- 与权限缓存相同，活动缓存失效也采用 `afterCommit`。
- 事务提交成功后再清理 Caffeine 和 Redis。
- 如果事务回滚，不清理缓存。

这样做的原因：

- 活动更新、报名、取消报名都会影响公开列表或详情中的状态、人数、容量等数据。
- 写库成功后删除缓存，下一次读请求会重新从 MySQL 加载最新数据。
- 采用 afterCommit 可以避免事务未提交时并发请求把旧活动数据重新写入缓存。

### 当前方案边界

当前二级缓存方案适合单体或少量实例部署，但仍有边界：

- Caffeine 是进程内缓存，多实例之间不会自动同步。
- 当前活动列表 Redis 清理使用 `keys hot:activity:list:*`，实现简单，但大规模生产环境应改为维护 key 集合、使用 scan，或通过缓存版本号规避全量 key 扫描。
- 角色权限变更时 `evictAll` 会清空所有用户权限快照，简单可靠，但角色权限频繁变更时会带来缓存抖动。当前系统角色权限变更属于低频管理操作，因此可以接受。

### 后续可升级方向

- 将 Caffeine 配置抽到 `application.yml`，支持按环境调整 TTL 和容量。
- 为 Caffeine 增加命中率、加载次数、淘汰次数等 Micrometer 指标。
- 多实例部署时，引入 Redis Pub/Sub 或 RabbitMQ 广播缓存失效事件。
- 将活动列表缓存从 `keys` 删除升级为缓存版本号策略，例如 `hot:activity:list:v{version}:...`。
- 对 dashboard overview 也可以增加 Caffeine L1，但需要继续保持“最近日志实时查询”的策略，避免用户误以为日志不更新。

## 2026-05-20：优惠券领取队列从 Redis List 迁移到 RabbitMQ

### 修改内容

本次将优惠券领取链路中的 Redis List 异步队列替换为 RabbitMQ：

- 新增 `spring-boot-starter-amqp` 依赖。
- 新增 RabbitMQ 配置：
  - exchange：`club.coupon.exchange`
  - queue：`club.coupon.claim.queue`
  - routing key：`coupon.claim`
- 新增 `RabbitMqConfig`，声明 durable direct exchange、durable queue 和 binding。
- 新增 `CouponClaimMessageProducer`，负责投递优惠券领取任务。
- `RedisCouponSeckillService` 保留 Redis Lua 的库存预扣和用户去重，但移除 `rpush coupon:claim:queue`。
- `CouponServiceImpl.claimCoupon` 在 Redis 预扣成功后创建 `coupon_claim_task`，并在事务提交后投递 RabbitMQ 消息。
- `CouponClaimWorker` 从定时 `leftPop Redis List` 改为 `@RabbitListener` 消费 RabbitMQ 消息。
- 保留 `compensateFailedTasks` 定时补偿任务，用于处理 RabbitMQ 投递失败、消费失败或服务重启后遗留的 `PENDING/FAILED` 任务。

### 新链路

```text
用户领取优惠券
  -> Redis Lua 原子判断库存和重复领取
  -> Redis 预扣库存、记录用户已占位
  -> MySQL 创建 coupon_claim_task
  -> 事务提交后发送 RabbitMQ 消息 taskId
  -> RabbitMQ Consumer 根据 taskId 查询任务
  -> 插入 user_coupon
  -> 更新 coupon_batch.claimed_count
  -> 更新 coupon_claim_task 为 DONE
```

### 策略抉择

没有把 Redis 整体替换为 RabbitMQ，而是只替换 Redis List 队列部分：

- Redis 继续负责高并发入口的原子预扣库存和用户去重。
- RabbitMQ 负责异步削峰、消费者解耦和更标准的消息队列能力。
- MySQL `coupon_claim_task` 表作为可靠任务表，兜底消息投递失败、消费失败和服务重启。
- `user_coupon` 的唯一索引继续作为最终防重复领取的数据库兜底。

这样做的原因：

- Redis Lua 更适合在高并发入口完成快速原子判断，避免每个请求都直接打 MySQL。
- Redis List 缺少标准 MQ 的消费确认、重试、死信等能力，不适合作为长期可靠队列。
- RabbitMQ 比 Redis List 更适合承载异步任务投递和消费解耦。
- 核心一致性不能只依赖 MQ，仍需要数据库唯一索引、任务表和补偿机制兜底。

### 消费幂等与补偿

`CouponClaimWorker` 消费 RabbitMQ 消息时只接收 `taskId`：

- 根据 `taskId` 查询 `coupon_claim_task`。
- 如果用户券已经存在，则直接将任务标记为 `DONE`。
- 插入 `user_coupon` 时捕获唯一键冲突，避免重复消费导致重复发券。
- 插入用户券成功后再更新批次已领取数，避免“先加计数、后插入失败”造成统计偏大。
- 如果处理失败，将任务标记为 `FAILED` 并增加重试次数。
- 定时补偿任务继续扫描 `PENDING/FAILED`，最多重试 10 次。

### 当前边界

- 当前尚未配置 RabbitMQ publisher confirm 和 dead-letter queue，可靠性主要由 `coupon_claim_task` 任务表和补偿任务兜底。
- 如果 RabbitMQ 暂时不可用，任务仍会留在 MySQL 中，补偿任务可以继续处理；但异步削峰能力会下降。
- 后续可以增加死信队列、消费重试间隔、失败告警和消息投递确认。

## 2026-05-20：新增 Docker Compose 一键部署

### 修改内容

本次新增容器化部署配置：

- 根目录新增 `docker-compose.yml`。
- 后端新增 `backend/Dockerfile` 和 `backend/.dockerignore`。
- 后端新增 `application-docker.yml`，容器内通过服务名连接 MySQL、Redis、RabbitMQ。
- 前端新增 `frontend/Dockerfile`、`frontend/.dockerignore` 和 `frontend/nginx.conf`。
- README 增加 Docker Compose 启动、查看日志、停止和清空数据卷说明。

### Compose 服务

```text
mysql      MySQL 8.4，业务库 club_system_plus
redis      Redis 7.4，缓存、限流和秒杀预扣库存
rabbitmq   RabbitMQ 4 Management，优惠券领取异步队列
backend    Spring Boot 后端，暴露 8080
frontend   Nginx 托管前端静态资源，暴露 5173
```

### 策略抉择

- 前端容器采用 Nginx 托管构建后的静态资源，而不是在容器里跑 Vite dev server。
- Nginx 将 `/api/**` 反向代理到 `backend:8080/api/**`，浏览器仍然只访问同源 `/api`。
- 后端单独提供 `docker` profile，避免容器环境继续连接本机 `localhost`。
- MySQL、Redis、RabbitMQ 都配置 healthcheck，后端等待基础设施健康后再启动。
- MySQL、Redis、RabbitMQ 使用 named volume 保存数据，避免容器重建后数据丢失。

### 当前边界

- 当前 Compose 面向本地演示和简历项目部署，不包含生产级 HTTPS、域名、镜像仓库和 CI/CD。
- MySQL root 密码仍写在示例配置中，真实生产环境应改为 `.env` 或密钥管理。
- 后续如果加入 MinIO，可以继续在 Compose 中增加 `minio` 服务，并将活动图片上传地址切换到对象存储。
