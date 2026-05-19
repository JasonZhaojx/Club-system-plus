alter database club_system_plus character set utf8mb4 collate utf8mb4_unicode_ci;

create table if not exists app_user (
    id bigint primary key auto_increment,
    username varchar(50) not null,
    password_hash varchar(100) not null,
    nickname varchar(50) not null,
    email varchar(120) null,
    status varchar(20) not null default 'NORMAL',
    created_at datetime not null default current_timestamp,
    updated_at datetime not null default current_timestamp on update current_timestamp,
    unique key uk_app_user_username (username),
    key idx_app_user_status (status)
);

create table if not exists role (
    id bigint primary key auto_increment,
    code varchar(50) not null,
    name varchar(50) not null,
    description varchar(255) null,
    created_at datetime not null default current_timestamp,
    updated_at datetime not null default current_timestamp on update current_timestamp,
    unique key uk_role_code (code)
);

create table if not exists permission (
    id bigint primary key auto_increment,
    code varchar(100) not null,
    name varchar(80) not null,
    description varchar(255) null,
    created_at datetime not null default current_timestamp,
    updated_at datetime not null default current_timestamp on update current_timestamp,
    unique key uk_permission_code (code)
);

create table if not exists user_role (
    id bigint primary key auto_increment,
    user_id bigint not null,
    role_id bigint not null,
    created_at datetime not null default current_timestamp,
    unique key uk_user_role (user_id, role_id),
    key idx_user_role_user_id (user_id),
    key idx_user_role_role_id (role_id)
);

create table if not exists role_permission (
    id bigint primary key auto_increment,
    role_id bigint not null,
    permission_id bigint not null,
    created_at datetime not null default current_timestamp,
    unique key uk_role_permission (role_id, permission_id),
    key idx_role_permission_role_id (role_id),
    key idx_role_permission_permission_id (permission_id)
);

create table if not exists department (
    id bigint primary key auto_increment,
    name varchar(80) not null,
    description varchar(255) null,
    status varchar(20) not null default 'ACTIVE',
    created_at datetime not null default current_timestamp,
    updated_at datetime not null default current_timestamp on update current_timestamp,
    unique key uk_department_name (name)
);

create table if not exists club_member (
    id bigint primary key auto_increment,
    user_id bigint not null,
    department_id bigint not null,
    joined_at datetime not null default current_timestamp,
    status varchar(20) not null default 'ACTIVE',
    created_at datetime not null default current_timestamp,
    updated_at datetime not null default current_timestamp on update current_timestamp,
    unique key uk_club_member_user (user_id),
    key idx_club_member_department_id (department_id),
    key idx_club_member_status (status),
    key idx_club_member_department_status (department_id, status)
);

create table if not exists department_leader (
    id bigint primary key auto_increment,
    user_id bigint not null,
    department_id bigint not null,
    appointed_at datetime not null default current_timestamp,
    created_at datetime not null default current_timestamp,
    unique key uk_department_leader (user_id, department_id),
    key idx_department_leader_user_id (user_id),
    key idx_department_leader_department_id (department_id)
);

create table if not exists activity (
    id bigint primary key auto_increment,
    title varchar(120) not null,
    summary varchar(255) not null,
    detail text not null,
    category varchar(50) not null,
    category_name varchar(50) not null,
    image_url varchar(500) null,
    location varchar(120) not null,
    start_time datetime not null,
    end_time datetime not null,
    capacity int not null,
    registered_count int not null default 0,
    status varchar(30) not null default 'DRAFT',
    required_role_code varchar(50) null,
    creator_id bigint null,
    reviewer_id bigint null,
    published_at datetime null,
    created_at datetime not null default current_timestamp,
    updated_at datetime not null default current_timestamp on update current_timestamp,
    key idx_activity_status (status),
    key idx_activity_category (category),
    key idx_activity_start_time (start_time),
    key idx_activity_public_query (status, category, start_time, id),
    key idx_activity_latest_query (status, published_at, id),
    key idx_activity_capacity_query (status, registered_count, capacity)
);

create table if not exists activity_registration (
    id bigint primary key auto_increment,
    activity_id bigint not null,
    user_id bigint not null,
    status varchar(20) not null default 'REGISTERED',
    registered_at datetime not null default current_timestamp,
    cancelled_at datetime null,
    created_at datetime not null default current_timestamp,
    updated_at datetime not null default current_timestamp on update current_timestamp,
    unique key uk_activity_registration_user (activity_id, user_id),
    key idx_activity_registration_user_id (user_id),
    key idx_activity_registration_status (status),
    key idx_activity_registration_activity_status (activity_id, status)
);

alter table app_user convert to character set utf8mb4 collate utf8mb4_unicode_ci;
alter table role convert to character set utf8mb4 collate utf8mb4_unicode_ci;
alter table permission convert to character set utf8mb4 collate utf8mb4_unicode_ci;
alter table department convert to character set utf8mb4 collate utf8mb4_unicode_ci;
alter table club_member convert to character set utf8mb4 collate utf8mb4_unicode_ci;
alter table department_leader convert to character set utf8mb4 collate utf8mb4_unicode_ci;
alter table activity convert to character set utf8mb4 collate utf8mb4_unicode_ci;
alter table activity_registration convert to character set utf8mb4 collate utf8mb4_unicode_ci;

insert ignore into role (code, name, description) values
('REGISTERED_USER', '注册用户', '已注册但不是社团成员'),
('CLUB_MEMBER', '普通成员', '社团普通成员'),
('DEPARTMENT_LEADER', '部门负责人', '管理本部门成员和活动审核'),
('PRESIDENT', '社长', '社团最高管理者'),
('SYSTEM_MAINTAINER', '系统维护者', '系统级维护和权限管理');

update role set name = '注册用户', description = '已注册但不是社团成员' where code = 'REGISTERED_USER';
update role set name = '普通成员', description = '社团普通成员' where code = 'CLUB_MEMBER';
update role set name = '部门负责人', description = '管理本部门成员和活动审核' where code = 'DEPARTMENT_LEADER';
update role set name = '社长', description = '社团最高管理者' where code = 'PRESIDENT';
update role set name = '系统维护者', description = '系统级维护和权限管理' where code = 'SYSTEM_MAINTAINER';

insert ignore into permission (code, name, description) values
('activity:view', '查看活动', '查看公开或授权活动'),
('activity:create', '创建活动', '创建活动草稿或申请'),
('activity:update', '修改活动', '修改活动或提交变更申请'),
('activity:cancel', '取消活动', '取消活动'),
('activity:review', '审核活动', '审核活动发布和变更申请'),
('coupon:grab', '抢券', '领取或抢购优惠券'),
('member:manage', '成员管理', '管理社团成员'),
('department:manage', '部门管理', '管理部门和负责人'),
('dashboard:view', '查看后台', '查看后台面板'),
('system:maintain', '系统维护', '维护系统配置、角色和权限');

update permission set name = '查看活动', description = '查看公开或授权活动' where code = 'activity:view';
update permission set name = '创建活动', description = '创建活动草稿或申请' where code = 'activity:create';
update permission set name = '修改活动', description = '修改活动或提交变更申请' where code = 'activity:update';
update permission set name = '取消活动', description = '取消活动' where code = 'activity:cancel';
update permission set name = '审核活动', description = '审核活动发布和变更申请' where code = 'activity:review';
update permission set name = '抢券', description = '领取或抢购优惠券' where code = 'coupon:grab';
update permission set name = '成员管理', description = '管理社团成员' where code = 'member:manage';
update permission set name = '部门管理', description = '管理部门和负责人' where code = 'department:manage';
update permission set name = '查看后台', description = '查看后台面板' where code = 'dashboard:view';
update permission set name = '系统维护', description = '维护系统配置、角色和权限' where code = 'system:maintain';

insert ignore into role_permission (role_id, permission_id)
select r.id, p.id
from role r
inner join permission p on p.code in ('activity:view', 'coupon:grab')
where r.code = 'REGISTERED_USER';

insert ignore into role_permission (role_id, permission_id)
select r.id, p.id
from role r
inner join permission p on p.code in ('activity:view', 'activity:create', 'activity:update', 'coupon:grab')
where r.code = 'CLUB_MEMBER';

insert ignore into role_permission (role_id, permission_id)
select r.id, p.id
from role r
inner join permission p on p.code in (
    'activity:view',
    'activity:create',
    'activity:update',
    'activity:review',
    'coupon:grab',
    'member:manage',
    'dashboard:view'
)
where r.code = 'DEPARTMENT_LEADER';

insert ignore into role_permission (role_id, permission_id)
select r.id, p.id
from role r
inner join permission p on p.code in (
    'activity:view',
    'activity:create',
    'activity:update',
    'activity:cancel',
    'activity:review',
    'coupon:grab',
    'member:manage',
    'department:manage',
    'dashboard:view'
)
where r.code = 'PRESIDENT';

insert ignore into role_permission (role_id, permission_id)
select r.id, p.id
from role r
cross join permission p
where r.code = 'SYSTEM_MAINTAINER';

insert into app_user (username, password_hash, nickname, email, status)
select 'root',
       '$2b$10$XrQ2RVf22LS/mkNhsyAijeFUYzExS4jpck0c90An8o.MAcJqd0zI2',
       'Root',
       null,
       'NORMAL'
where not exists (
    select 1
    from app_user
    where username = 'root'
);

insert ignore into user_role (user_id, role_id)
select u.id, r.id
from app_user u
inner join role r on r.code = 'SYSTEM_MAINTAINER'
where u.username = 'root';

insert ignore into user_role (user_id, role_id)
select u.id, r.id
from app_user u
inner join role r on r.code = 'REGISTERED_USER'
left join user_role ur on ur.user_id = u.id
where u.username <> 'root'
  and ur.id is null;

insert ignore into activity (
    id, title, summary, detail, category, category_name, image_url, location,
    start_time, end_time, capacity, registered_count, status, required_role_code, creator_id, published_at
) values
(1, 'AI 应用工作坊', '从知识库设计到前端原型，完成一次 AI 应用从 0 到 1 的实践。', '本次工作坊围绕 AI 应用完整流程展开，成员将分组完成用户问题定义、知识库结构设计、Prompt 评审和前端原型演示。活动结束后会进行方案复盘，帮助成员把课程内容转化为可展示作品。', 'technology', '技术工作坊', 'https://images.unsplash.com/photo-1519389950473-47ba0277781c?auto=format&fit=crop&w=1200&q=80', '创新中心 A201', '2026-06-05 14:00:00', '2026-06-05 17:00:00', 80, 0, 'PUBLISHED', null, 1, current_timestamp),
(2, 'Hack Night 校园产品挑战', '限时交付校园服务产品，覆盖产品、设计、工程和展示。', 'Hack Night 以限时交付为核心，题目覆盖校园服务、活动推荐、学习助手和社团运营工具。现场提供技术导师支持，最终以产品演示、代码质量和用户价值三个维度评选优秀团队。', 'competition', '竞赛挑战', 'https://images.unsplash.com/photo-1504384308090-c894fdcc538d?auto=format&fit=crop&w=1200&q=80', '工程楼路演厅', '2026-06-12 18:30:00', '2026-06-12 22:30:00', 120, 0, 'PUBLISHED', 'CLUB_MEMBER', 1, current_timestamp),
(3, '职业分享：从校园项目到真实业务', '邀请校友分享项目作品、面试表达和真实业务协作经验。', '活动邀请创业校友和企业工程师分享从校园 idea 到真实产品的过程，包括用户访谈、MVP 搭建、工程协作、简历表达和面试中的项目讲述方式。', 'career', '职业分享', 'https://images.unsplash.com/photo-1556761175-b413da4baf72?auto=format&fit=crop&w=1200&q=80', '商学院报告厅', '2026-06-20 19:00:00', '2026-06-20 21:00:00', 200, 0, 'PUBLISHED', null, 1, current_timestamp),
(4, 'Community Day 社团开放日', '了解部门方向、项目成果和新成员加入流程。', 'Community Day 面向所有学生开放。技术部、运营部、设计部和外联部展示过往项目成果，并提供部门咨询。新成员可以现场了解加入流程和后续活动安排。', 'community', '社群活动', 'https://images.unsplash.com/photo-1521737604893-d14cc237f11d?auto=format&fit=crop&w=1200&q=80', '学生活动中心', '2026-07-02 13:30:00', '2026-07-02 16:30:00', 160, 0, 'PUBLISHED', null, 1, current_timestamp);

update activity
set title = 'AI 应用工作坊',
    summary = '从知识库设计到前端原型，完成一次 AI 应用从 0 到 1 的实践。',
    detail = '本次工作坊围绕 AI 应用完整流程展开，成员将分组完成用户问题定义、知识库结构设计、Prompt 评审和前端原型演示。活动结束后会进行方案复盘，帮助成员把课程内容转化为可展示作品。',
    category = 'technology',
    category_name = '技术工作坊',
    image_url = 'https://images.unsplash.com/photo-1519389950473-47ba0277781c?auto=format&fit=crop&w=1200&q=80',
    location = '创新中心 A201'
where id = 1;

update activity
set title = 'Hack Night 校园产品挑战',
    summary = '限时交付校园服务产品，覆盖产品、设计、工程和展示。',
    detail = 'Hack Night 以限时交付为核心，题目覆盖校园服务、活动推荐、学习助手和社团运营工具。现场提供技术导师支持，最终以产品演示、代码质量和用户价值三个维度评选优秀团队。',
    category = 'competition',
    category_name = '竞赛挑战',
    image_url = 'https://images.unsplash.com/photo-1504384308090-c894fdcc538d?auto=format&fit=crop&w=1200&q=80',
    location = '工程楼路演厅'
where id = 2;

update activity
set title = '职业分享：从校园项目到真实业务',
    summary = '邀请校友分享项目作品、面试表达和真实业务协作经验。',
    detail = '活动邀请创业校友和企业工程师分享从校园 idea 到真实产品的过程，包括用户访谈、MVP 搭建、工程协作、简历表达和面试中的项目讲述方式。',
    category = 'career',
    category_name = '职业分享',
    image_url = 'https://images.unsplash.com/photo-1556761175-b413da4baf72?auto=format&fit=crop&w=1200&q=80',
    location = '商学院报告厅'
where id = 3;

update activity
set title = 'Community Day 社团开放日',
    summary = '了解部门方向、项目成果和新成员加入流程。',
    detail = 'Community Day 面向所有学生开放。技术部、运营部、设计部和外联部展示过往项目成果，并提供部门咨询。新成员可以现场了解加入流程和后续活动安排。',
    category = 'community',
    category_name = '社群活动',
    image_url = 'https://images.unsplash.com/photo-1521737604893-d14cc237f11d?auto=format&fit=crop&w=1200&q=80',
    location = '学生活动中心'
where id = 4;

create table if not exists coupon_batch (
    id bigint primary key auto_increment,
    name varchar(120) not null,
    description varchar(255) null,
    coupon_type varchar(50) not null,
    benefit_text varchar(120) not null,
    stock int not null,
    claimed_count int not null default 0,
    claim_start_time datetime not null,
    claim_end_time datetime not null,
    expire_time datetime not null,
    allowed_role_codes varchar(255) null,
    status varchar(30) not null default 'ACTIVE',
    creator_id bigint null,
    created_at datetime not null default current_timestamp,
    updated_at datetime not null default current_timestamp on update current_timestamp,
    key idx_coupon_batch_status (status),
    key idx_coupon_batch_claim_time (claim_start_time, claim_end_time),
    key idx_coupon_batch_status_created (status, created_at, id),
    key idx_coupon_batch_active_window (status, claim_start_time, claim_end_time, expire_time)
);

create table if not exists user_coupon (
    id bigint primary key auto_increment,
    batch_id bigint not null,
    user_id bigint not null,
    status varchar(20) not null default 'UNUSED',
    claimed_at datetime not null default current_timestamp,
    used_at datetime null,
    created_at datetime not null default current_timestamp,
    updated_at datetime not null default current_timestamp on update current_timestamp,
    unique key uk_user_coupon_batch_user (batch_id, user_id),
    key idx_user_coupon_user_id (user_id),
    key idx_user_coupon_status (status),
    key idx_user_coupon_user_status (user_id, status),
    key idx_user_coupon_batch_status (batch_id, status)
);

create table if not exists coupon_redemption (
    id bigint primary key auto_increment,
    user_coupon_id bigint not null,
    batch_id bigint not null,
    user_id bigint not null,
    scene varchar(80) null,
    note varchar(255) null,
    redeemed_at datetime not null default current_timestamp,
    created_at datetime not null default current_timestamp,
    key idx_coupon_redemption_user_id (user_id),
    key idx_coupon_redemption_batch_id (batch_id)
);

create table if not exists coupon_claim_task (
    id bigint primary key auto_increment,
    batch_id bigint not null,
    user_id bigint not null,
    status varchar(20) not null default 'PENDING',
    retry_count int not null default 0,
    error_message varchar(500) null,
    created_at datetime not null default current_timestamp,
    updated_at datetime not null default current_timestamp on update current_timestamp,
    unique key uk_coupon_claim_task_user (batch_id, user_id),
    key idx_coupon_claim_task_status (status, retry_count, updated_at)
);

alter table coupon_batch convert to character set utf8mb4 collate utf8mb4_unicode_ci;
alter table user_coupon convert to character set utf8mb4 collate utf8mb4_unicode_ci;
alter table coupon_redemption convert to character set utf8mb4 collate utf8mb4_unicode_ci;
alter table coupon_claim_task convert to character set utf8mb4 collate utf8mb4_unicode_ci;

create table if not exists api_access_log (
    id bigint primary key auto_increment,
    method varchar(10) not null,
    path varchar(255) not null,
    status_code int not null,
    duration_ms bigint not null,
    user_id bigint null,
    username varchar(50) null,
    ip_address varchar(64) null,
    user_agent varchar(500) null,
    created_at datetime not null default current_timestamp,
    key idx_api_access_log_created_at (created_at),
    key idx_api_access_log_path_created_at (path, created_at),
    key idx_api_access_log_status_created_at (status_code, created_at)
);

create table if not exists operation_log (
    id bigint primary key auto_increment,
    user_id bigint null,
    username varchar(50) null,
    method varchar(10) not null,
    path varchar(255) not null,
    action varchar(120) not null,
    status_code int not null,
    duration_ms bigint not null,
    ip_address varchar(64) null,
    created_at datetime not null default current_timestamp,
    key idx_operation_log_created_at (created_at),
    key idx_operation_log_user_created_at (user_id, created_at),
    key idx_operation_log_action_created_at (action, created_at)
);

alter table api_access_log convert to character set utf8mb4 collate utf8mb4_unicode_ci;
alter table operation_log convert to character set utf8mb4 collate utf8mb4_unicode_ci;

create table if not exists api_access_minute_stat (
    stat_minute datetime primary key,
    total_count bigint not null default 0,
    error_count bigint not null default 0,
    avg_duration_ms bigint not null default 0,
    updated_at datetime not null default current_timestamp on update current_timestamp
);

create table if not exists api_path_hour_stat (
    stat_hour datetime not null,
    method varchar(10) not null,
    path varchar(255) not null,
    total_count bigint not null default 0,
    error_count bigint not null default 0,
    avg_duration_ms bigint not null default 0,
    max_status_code int not null default 200,
    updated_at datetime not null default current_timestamp on update current_timestamp,
    primary key (stat_hour, method, path),
    key idx_api_path_hour_stat_path (path, stat_hour),
    key idx_api_path_hour_stat_total (stat_hour, total_count),
    key idx_api_path_hour_stat_error (stat_hour, error_count)
);

create table if not exists user_activity_day_stat (
    stat_date date not null,
    user_id bigint not null,
    username varchar(50) null,
    total_count bigint not null default 0,
    operation_count bigint not null default 0,
    updated_at datetime not null default current_timestamp on update current_timestamp,
    primary key (stat_date, user_id),
    key idx_user_activity_day_stat_total (stat_date, total_count)
);

alter table api_access_minute_stat convert to character set utf8mb4 collate utf8mb4_unicode_ci;
alter table api_path_hour_stat convert to character set utf8mb4 collate utf8mb4_unicode_ci;
alter table user_activity_day_stat convert to character set utf8mb4 collate utf8mb4_unicode_ci;

insert ignore into permission (code, name, description) values
('coupon:manage', '管理优惠券', '创建和管理优惠券批次');

insert ignore into role_permission (role_id, permission_id)
select r.id, p.id
from role r
inner join permission p on p.code = 'coupon:manage'
where r.code in ('DEPARTMENT_LEADER', 'PRESIDENT', 'SYSTEM_MAINTAINER');

insert ignore into coupon_batch (
    id, name, description, coupon_type, benefit_text, stock, claimed_count,
    claim_start_time, claim_end_time, expire_time, allowed_role_codes, status, creator_id
) values
(1, '新成员活动权益券', '可用于线下活动物料、报名费用或门票权益兑换。', 'BENEFIT', '活动物料或门票权益', 100, 0,
 '2026-05-01 00:00:00', '2026-12-31 23:59:59', '2027-01-31 23:59:59',
 'REGISTERED_USER,CLUB_MEMBER,DEPARTMENT_LEADER,PRESIDENT', 'ACTIVE', 1);
