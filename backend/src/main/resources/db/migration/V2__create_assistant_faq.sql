create table if not exists assistant_faq (
    id bigint primary key auto_increment,
    question varchar(255) not null,
    answer text not null,
    category varchar(50) not null,
    enabled tinyint(1) not null default 1,
    created_at datetime not null default current_timestamp,
    updated_at datetime not null default current_timestamp on update current_timestamp,
    key idx_assistant_faq_enabled_category (enabled, category)
);

alter table assistant_faq convert to character set utf8mb4 collate utf8mb4_unicode_ci;

insert into assistant_faq (question, answer, category, enabled)
select '如何加入社团？',
       '你可以先注册账号，关注公开活动和部门介绍。若系统开放成员申请，可按页面提示提交申请；也可以参加开放日或联系社团负责人了解加入流程。',
       'membership',
       1
where not exists (select 1 from assistant_faq where question = '如何加入社团？');

insert into assistant_faq (question, answer, category, enabled)
select '活动报名后可以取消吗？',
       '可以。已登录用户可在“我的活动”中查看报名记录，并在活动允许的情况下取消报名。',
       'activity',
       1
where not exists (select 1 from assistant_faq where question = '活动报名后可以取消吗？');

insert into assistant_faq (question, answer, category, enabled)
select '优惠券怎么使用？',
       '领取后可在“我的优惠券”中查看状态、有效期和权益说明。使用时按页面提示核销，具体可用场景以优惠券说明为准。',
       'coupon',
       1
where not exists (select 1 from assistant_faq where question = '优惠券怎么使用？');

insert into assistant_faq (question, answer, category, enabled)
select '社团有哪些常见角色？',
       '系统中常见角色包括注册用户、普通成员、部门负责人、社长和系统维护者。不同角色拥有不同的活动、优惠券、成员和后台管理权限。',
       'rbac',
       1
where not exists (select 1 from assistant_faq where question = '社团有哪些常见角色？');
