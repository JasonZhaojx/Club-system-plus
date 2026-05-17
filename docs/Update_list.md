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
