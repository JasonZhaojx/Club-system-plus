alter database club_system_plus character set utf8mb4 collate utf8mb4_unicode_ci;

create table if not exists app_user (
    id bigint primary key auto_increment,
    username varchar(50) not null,
    password_hash varchar(100) not null,
    nickname varchar(50) not null,
    email varchar(120) null,
    avatar_url varchar(500) not null default 'https://ts1.tc.mm.bing.net/th/id/OIP-C.4n3KcdpOWTC32-U0LjDagwHaHa?cb=thfc1falcon&rs=1&pid=ImgDetMain&o=7&rm=3',
    status varchar(20) not null default 'NORMAL',
    token_version int not null default 0,
    created_at datetime not null default current_timestamp,
    updated_at datetime not null default current_timestamp on update current_timestamp,
    unique key uk_app_user_username (username),
    key idx_app_user_email (email),
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
('coupon:manage', '优惠券管理', '创建、修改和查询优惠券批次'),
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
update permission set name = '优惠券管理', description = '创建、修改和查询优惠券批次' where code = 'coupon:manage';
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
    'coupon:manage',
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

insert into app_user (username, password_hash, nickname, email, avatar_url, status)
select 'root',
       '$2b$10$XrQ2RVf22LS/mkNhsyAijeFUYzExS4jpck0c90An8o.MAcJqd0zI2',
       'Root',
       null,
       'https://ts1.tc.mm.bing.net/th/id/OIP-C.4n3KcdpOWTC32-U0LjDagwHaHa?cb=thfc1falcon&rs=1&pid=ImgDetMain&o=7&rm=3',
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

insert into activity (
    id, title, summary, detail, category, category_name, image_url, review_image_url, review_content, location,
    start_time, end_time, capacity, registered_count, status, required_role_code, creator_id, published_at
) values
(1, '学期迎新摆摊和派对', '每学期开端的标志性活动，为新生和返校同学提供轻松愉快的社交平台。', '每学期开端的标志性活动，为新生和返校同学提供轻松愉快的社交平台。美食、游戏与音乐，精彩大学旅程从这里开启。', 'social', '娱乐·交友', 'https://images.unsplash.com/photo-1523580494863-6f3031224c94?auto=format&fit=crop&w=1200&q=80', null, null, 'UNSW Kensington 校园主广场', '2026-07-20 12:00:00', '2026-07-20 18:00:00', 1500, 320, 'PUBLISHED', null, 1, current_timestamp),
(2, '学期期末加油包', '每到期末，学联为同学们精心准备加油包，为辛苦备考的同学送上温暖与支持。', '每到期末，学联为同学们精心准备加油包，内含零食、饮料、护肤品等，为辛苦备考的同学送上温暖与支持。', 'welfare', '关怀·支持', 'https://images.unsplash.com/photo-1511988617509-a57c8a288659?auto=format&fit=crop&w=1200&q=80', null, null, 'UNSW Library Walkway', '2026-06-24 11:00:00', '2026-06-24 15:00:00', 600, 180, 'PUBLISHED', null, 1, current_timestamp),
(3, '周常社交运动局', '涵盖桌游、篮球、排球和足球，为同学们提供多种运动与社交机会。', '涵盖桌游、篮球、排球和足球，为同学们提供多种运动与社交的机会。每周都有活动，放松身心，结交新朋友。', 'sports', '体育·社交', 'https://images.unsplash.com/photo-1546519638-68e109498ffc?auto=format&fit=crop&w=1200&q=80', null, null, 'UNSW Fitness & Aquatic Centre', '2026-06-28 17:30:00', '2026-06-28 20:00:00', 120, 58, 'PUBLISHED', null, 1, current_timestamp),
(4, '文化美食夜市摆摊', '汇聚中华美食与文化，在校园中重现夜市热闹氛围。', '汇聚中华美食与文化，在校园中重现夜市热闹氛围，让同学们在异乡感受熟悉的文化温度。', 'culture', '文化活动', 'https://images.unsplash.com/photo-1555939594-58d7cb561ad1?auto=format&fit=crop&w=1200&q=80', null, null, 'UNSW Village Green', '2026-08-15 17:00:00', '2026-08-15 21:30:00', 900, 260, 'PUBLISHED', null, 1, current_timestamp),
(5, '汪汪解压局', '在学业压力最大的时候，和可爱的小狗们共度时光，用最治愈的方式放松心情。', '在学业压力最大的时候，和可爱的小狗们共度时光，用最治愈的方式放松心情，重拾能量。', 'welfare', '解压·治愈', 'https://images.unsplash.com/photo-1548199973-03cce0bbc87b?auto=format&fit=crop&w=1200&q=80', null, null, 'UNSW Main Library Lawn', '2026-07-03 13:00:00', '2026-07-03 16:00:00', 220, 76, 'PUBLISHED', null, 1, current_timestamp),
(6, '职规 Networking & Peer Mentoring', '连接学长学姐与业界精英，提供职业规划指导、简历分享及企业招聘交流。', '连接学长学姐与业界精英，提供职业规划指导、简历撰写分享及企业招聘会，助力你的职业发展。', 'career', '职业·学术', 'https://images.unsplash.com/photo-1551836022-d5d88e9218df?auto=format&fit=crop&w=1200&q=80', null, null, 'UNSW Business School Theatre', '2026-07-10 18:30:00', '2026-07-10 21:00:00', 260, 88, 'PUBLISHED', null, 1, current_timestamp),
(7, '澳洲八大新生行前会', '由 UNSW 中国学联牵头发起，联合澳洲八大高校学联共同打造的八大联合国内行前项目。', '由 UNSW 中国学联牵头发起，联合澳洲八大高校学联共同打造的首个八大联合国内行前项目。在广州、上海、北京三城重磅落地，吸引超过 500 名新生及家长参与。', 'major', '全澳首创', 'https://images.unsplash.com/photo-1523240795612-9a054b0db644?auto=format&fit=crop&w=1200&q=80', 'https://images.unsplash.com/photo-1523240795612-9a054b0db644?auto=format&fit=crop&w=1200&q=80', '广州、上海、北京三城落地，帮助新生和家长提前了解澳洲学习生活、入学准备和校园资源。', '广州 · 上海 · 北京', '2025-07-20 14:00:00', '2025-07-28 18:00:00', 500, 500, 'ENDED', null, 1, current_timestamp),
(8, '南半球官方电竞比赛', '联合腾讯官方合作落地举办，面向南半球所有学生开放，打造高规格电竞交流平台。', '联合腾讯官方合作落地举办，赛事涵盖《金铲铲之战》与《王者荣耀》两大热门项目。面向南半球所有学生开放，打造高规格电竞交流平台。', 'major', '腾讯官方合作', 'https://images.unsplash.com/photo-1542751371-adc38448a05e?auto=format&fit=crop&w=1200&q=80', 'https://images.unsplash.com/photo-1542751371-adc38448a05e?auto=format&fit=crop&w=1200&q=80', '赛事覆盖金铲铲之战与王者荣耀，连接南半球学生玩家与官方赛事资源。', '线上赛事 · UNSW 线下观赛点', '2025-08-16 12:00:00', '2025-08-18 22:00:00', 800, 800, 'ENDED', null, 1, current_timestamp),
(9, '留学人员中秋国庆晚会', '2500 余名在新州求学的留学生齐聚悉尼市政厅，共同点亮海外学子的中秋团圆夜。', '2025 年 9 月 27 日，2500 余名在新州求学的留学生齐聚悉尼市政厅，共同点亮这场属于海外学子的中秋团圆夜。中国驻悉尼总领馆共同出席见证。', 'major', '2500+ 人', 'https://images.unsplash.com/photo-1511795409834-ef04bbd61622?auto=format&fit=crop&w=1200&q=80', 'https://images.unsplash.com/photo-1511795409834-ef04bbd61622?auto=format&fit=crop&w=1200&q=80', '悉尼市政厅中秋国庆晚会承载海外学子的团圆记忆，融合舞台表演、文化展示与社区连接。', '悉尼市政厅', '2025-09-27 18:30:00', '2025-09-27 22:30:00', 2500, 2500, 'ENDED', null, 1, current_timestamp),
(10, '「月下巡航」万圣节游轮派对', '在悉尼港湾璀璨夜景中举办万圣节游轮派对，500 余名同学共度难忘一夜。', '在悉尼港湾璀璨夜景中举办万圣节游轮派对，500 余名同学在 Starship 上共度难忘一夜，悉尼标志性风景成为最浪漫的舞台背景。', 'major', '年终盛典', 'https://images.unsplash.com/photo-1507525428034-b723cf961d3e?auto=format&fit=crop&w=1200&q=80', 'https://images.unsplash.com/photo-1507525428034-b723cf961d3e?auto=format&fit=crop&w=1200&q=80', 'Starship 游轮、悉尼港夜景与万圣节主题派对组成属于新南同学的年终盛典。', 'Starship · Sydney Harbour', '2024-11-03 19:00:00', '2024-11-03 23:00:00', 500, 500, 'ENDED', null, 1, current_timestamp)
on duplicate key update
    title = values(title),
    summary = values(summary),
    detail = values(detail),
    category = values(category),
    category_name = values(category_name),
    image_url = values(image_url),
    review_image_url = values(review_image_url),
    review_content = values(review_content),
    location = values(location),
    start_time = values(start_time),
    end_time = values(end_time),
    capacity = values(capacity),
    registered_count = values(registered_count),
    status = values(status),
    required_role_code = values(required_role_code),
    published_at = values(published_at);


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

