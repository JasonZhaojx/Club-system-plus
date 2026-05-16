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
    key idx_club_member_status (status)
);

insert ignore into role (code, name, description) values
('REGISTERED_USER', '注册用户', '已注册但不是社团成员'),
('CLUB_MEMBER', '普通成员', '社团普通成员'),
('DEPARTMENT_LEADER', '部门负责人', '管理本部门成员和活动审核'),
('PRESIDENT', '社长', '社团最高管理者'),
('SYSTEM_MAINTAINER', '系统维护者', '系统级维护和权限管理');

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

insert ignore into app_user (username, password_hash, nickname, email, status) values
('root', '$2b$10$XrQ2RVf22LS/mkNhsyAijeFUYzExS4jpck0c90An8o.MAcJqd0zI2', 'Root', null, 'NORMAL');

insert ignore into user_role (user_id, role_id)
select u.id, r.id
from app_user u
inner join role r on r.code = 'SYSTEM_MAINTAINER'
where u.username = 'root';

insert ignore into user_role (user_id, role_id)
select u.id, r.id
from app_user u
inner join role r on r.code = 'CLUB_MEMBER'
left join user_role ur on ur.user_id = u.id
where u.username <> 'root'
  and ur.id is null;
