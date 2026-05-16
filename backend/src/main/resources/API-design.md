# Club System Plus API Design

## 1. 基础约定

- Base URL: `/api`
- OpenAPI: `/api/v3/api-docs`
- Swagger UI: `/api/swagger-ui.html`
- Content-Type: `application/json`
- 认证方式: `Authorization: Bearer <access_token>`

## 2. 统一响应结构

所有接口统一返回 `Result<T>`:

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

常用状态码:

| code | 含义 |
| --- | --- |
| 0 | 成功 |
| 400 | 请求参数错误 |
| 401 | 未登录或登录已过期 |
| 403 | 无权限访问 |
| 404 | 资源不存在 |
| 409 | 业务冲突 |
| 500 | 系统异常 |

分页响应建议:

```json
{
  "records": [],
  "total": 0,
  "page": 1,
  "size": 10
}
```

## 3. 系统接口

| 方法 | 路径 | 说明 | 权限 |
| --- | --- | --- | --- |
| GET | `/health` | 健康检查 | 公开 |

响应:

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "status": "UP",
    "time": "2026-05-15T21:00:00+08:00"
  }
}
```

## 4. 认证与用户

| 方法 | 路径 | 说明 | 权限 |
| --- | --- | --- | --- |
| POST | `/auth/register` | 用户注册 | 公开 |
| POST | `/auth/login` | 用户登录 | 公开 |
| POST | `/auth/logout` | 用户退出 | 登录用户 |
| GET | `/users/me` | 当前用户信息 | 登录用户 |
| PUT | `/users/me` | 更新当前用户资料 | 登录用户 |
| GET | `/users/me/registrations` | 我的报名记录 | 登录用户 |
| GET | `/users/me/coupons` | 我的优惠券 | 登录用户 |

注册请求:

```json
{
  "username": "tom",
  "email": "tom@example.com",
  "phone": "13800000000",
  "password": "Password123"
}
```

登录响应 `data`:

```json
{
  "accessToken": "jwt-token",
  "user": {
    "id": 1,
    "username": "tom",
    "roles": ["REGISTERED_USER"]
  }
}
```

## 5. 官网展示

| 方法 | 路径 | 说明 | 权限 |
| --- | --- | --- | --- |
| GET | `/site/profile` | 社团介绍 | 公开 |
| GET | `/site/leaders` | 重要人员列表 | 公开 |
| GET | `/departments/public` | 公开部门列表 | 公开 |
| GET | `/departments/public/{id}` | 公开部门详情 | 公开 |

## 6. 活动

| 方法 | 路径 | 说明 | 权限 |
| --- | --- | --- | --- |
| GET | `/activities` | 活动列表 | 公开/登录后按权限过滤 |
| GET | `/activities/{id}` | 活动详情 | 公开/登录后按权限过滤 |
| POST | `/activities/{id}/registrations` | 报名活动 | 登录用户 |
| DELETE | `/activities/{id}/registrations/me` | 取消我的报名 | 登录用户 |
| POST | `/member/activity-requests` | 创建活动申请 | 社团成员及以上 |
| GET | `/member/activity-requests` | 我的活动申请 | 社团成员及以上 |

活动列表查询参数:

| 参数 | 类型 | 说明 |
| --- | --- | --- |
| page | number | 页码，从 1 开始 |
| size | number | 每页数量 |
| keyword | string | 关键词 |
| type | string | 活动类型 |
| status | string | 活动状态 |

## 7. 审核中心

| 方法 | 路径 | 说明 | 权限 |
| --- | --- | --- | --- |
| GET | `/admin/reviews/activity-requests` | 待审核活动申请 | 部门负责人及以上 |
| POST | `/admin/reviews/activity-requests/{id}/approve` | 通过活动申请 | 部门负责人及以上 |
| POST | `/admin/reviews/activity-requests/{id}/reject` | 拒绝活动申请 | 部门负责人及以上 |

拒绝请求:

```json
{
  "reason": "活动时间与已有活动冲突"
}
```

## 8. 成员与部门

| 方法 | 路径 | 说明 | 权限 |
| --- | --- | --- | --- |
| GET | `/admin/members` | 成员列表 | 部门负责人及以上 |
| POST | `/admin/members` | 添加成员 | 部门负责人及以上 |
| PUT | `/admin/members/{id}` | 更新成员 | 部门负责人及以上 |
| DELETE | `/admin/members/{id}` | 移除成员 | 部门负责人及以上 |
| GET | `/admin/departments` | 部门列表 | 社长/维护者 |
| POST | `/admin/departments` | 创建部门 | 社长/维护者 |
| PUT | `/admin/departments/{id}` | 更新部门 | 社长/维护者 |
| PATCH | `/admin/departments/{id}/disable` | 停用部门 | 社长/维护者 |
| POST | `/admin/departments/{id}/leaders` | 任命部门负责人 | 社长/维护者 |
| DELETE | `/admin/departments/{id}/leaders/{userId}` | 移除部门负责人 | 社长/维护者 |

## 9. 优惠券

| 方法 | 路径 | 说明 | 权限 |
| --- | --- | --- | --- |
| GET | `/coupons` | 可领取优惠券批次 | 登录用户 |
| POST | `/coupons/{batchId}/claim` | 领取优惠券 | 登录用户 |
| GET | `/admin/coupon-batches` | 优惠券批次列表 | 社长/维护者 |
| POST | `/admin/coupon-batches` | 创建优惠券批次 | 社长/维护者 |
| PUT | `/admin/coupon-batches/{id}` | 更新优惠券批次 | 社长/维护者 |
| PATCH | `/admin/coupon-batches/{id}/disable` | 停用优惠券批次 | 社长/维护者 |

## 10. 数据面板与系统维护

| 方法 | 路径 | 说明 | 权限 |
| --- | --- | --- | --- |
| GET | `/admin/dashboard/overview` | 运营指标总览 | 社长/维护者 |
| GET | `/admin/dashboard/api-traffic` | 接口访问趋势 | 维护者 |
| GET | `/admin/system/configs` | 系统配置列表 | 维护者 |
| PUT | `/admin/system/configs/{key}` | 更新系统配置 | 维护者 |

## 11. 前端路由映射

| 前端路由 | 对应接口模块 |
| --- | --- |
| `/` | `/site/profile`, `/activities` |
| `/login` | `/auth/login` |
| `/activities` | `/activities` |
| `/activities/:id` | `/activities/{id}`, `/activities/{id}/registrations` |
| `/user/profile` | `/users/me` |
| `/user/registrations` | `/users/me/registrations` |
| `/user/coupons` | `/users/me/coupons` |
| `/member/activity-requests` | `/member/activity-requests` |
| `/admin` | `/admin/dashboard/overview` |
| `/admin/reviews` | `/admin/reviews/activity-requests` |
| `/admin/members` | `/admin/members` |
| `/admin/departments` | `/admin/departments` |
| `/admin/coupons` | `/admin/coupon-batches` |
