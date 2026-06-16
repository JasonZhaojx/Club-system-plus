delete from activity_registration
where activity_id in (
    select id
    from activity
    where title not in (
        '学期迎新摆摊和派对',
        '学期期末加油包',
        '周常社交运动局',
        '文化美食夜市摆摊',
        '汪汪解压局',
        '职规Networking & Peer Mentoring',
        '澳洲八大新生行前会',
        '南半球官方电竞比赛',
        '留学人员中秋国庆晚会',
        '「月下巡航」万圣节游轮派对'
    )
);

delete from activity
where title not in (
    '学期迎新摆摊和派对',
    '学期期末加油包',
    '周常社交运动局',
    '文化美食夜市摆摊',
    '汪汪解压局',
    '职规Networking & Peer Mentoring',
    '澳洲八大新生行前会',
    '南半球官方电竞比赛',
    '留学人员中秋国庆晚会',
    '「月下巡航」万圣节游轮派对'
);

insert into activity (
    title, summary, detail, category, category_name, image_url, review_image_url, review_content, location,
    start_time, end_time, capacity, registered_count, status, required_role_code, creator_id, published_at
)
select
    '学期迎新摆摊和派对',
    '每学期开端的标志性活动，为新生和返校同学提供轻松愉快的社交平台。',
    '每学期开端的标志性活动，为新生和返校同学提供轻松愉快的社交平台。美食、游戏与音乐，精彩大学旅程从这里开启。',
    'social',
    '娱乐·交友',
    'https://images.unsplash.com/photo-1523580494863-6f3031224c94?auto=format&fit=crop&w=1200&q=80',
    null,
    null,
    'UNSW Kensington 校园主广场',
    '2026-07-20 12:00:00',
    '2026-07-20 18:00:00',
    1500,
    320,
    'PUBLISHED',
    null,
    1,
    current_timestamp
where not exists (select 1 from activity where title = '学期迎新摆摊和派对');

insert into activity (
    title, summary, detail, category, category_name, image_url, review_image_url, review_content, location,
    start_time, end_time, capacity, registered_count, status, required_role_code, creator_id, published_at
)
select
    '学期期末加油包',
    '每到期末，学联为同学们精心准备加油包，为辛苦备考的同学送上温暖与支持。',
    '每到期末，学联为同学们精心准备加油包，内含零食、饮料、护肤品等，为辛苦备考的同学送上温暖与支持。',
    'welfare',
    '关怀·支持',
    'https://images.unsplash.com/photo-1511988617509-a57c8a288659?auto=format&fit=crop&w=1200&q=80',
    null,
    null,
    'UNSW Library Walkway',
    '2026-06-24 11:00:00',
    '2026-06-24 15:00:00',
    600,
    180,
    'PUBLISHED',
    null,
    1,
    current_timestamp
where not exists (select 1 from activity where title = '学期期末加油包');

insert into activity (
    title, summary, detail, category, category_name, image_url, review_image_url, review_content, location,
    start_time, end_time, capacity, registered_count, status, required_role_code, creator_id, published_at
)
select
    '周常社交运动局',
    '涵盖桌游、篮球、排球和足球，为同学们提供多种运动与社交机会。',
    '涵盖桌游、篮球、排球和足球，为同学们提供多种运动与社交的机会。每周都有活动，放松身心，结交新朋友。',
    'sports',
    '体育·社交',
    'https://images.unsplash.com/photo-1546519638-68e109498ffc?auto=format&fit=crop&w=1200&q=80',
    null,
    null,
    'UNSW Fitness & Aquatic Centre',
    '2026-06-28 17:30:00',
    '2026-06-28 20:00:00',
    120,
    58,
    'PUBLISHED',
    null,
    1,
    current_timestamp
where not exists (select 1 from activity where title = '周常社交运动局');

insert into activity (
    title, summary, detail, category, category_name, image_url, review_image_url, review_content, location,
    start_time, end_time, capacity, registered_count, status, required_role_code, creator_id, published_at
)
select
    '文化美食夜市摆摊',
    '汇聚中华美食与文化，在校园中重现夜市热闹氛围。',
    '汇聚中华美食与文化，在校园中重现夜市热闹氛围，让同学们在异乡感受熟悉的文化温度。',
    'culture',
    '文化',
    'https://images.unsplash.com/photo-1555939594-58d7cb561ad1?auto=format&fit=crop&w=1200&q=80',
    null,
    null,
    'UNSW Village Green',
    '2026-08-15 17:00:00',
    '2026-08-15 21:30:00',
    900,
    260,
    'PUBLISHED',
    null,
    1,
    current_timestamp
where not exists (select 1 from activity where title = '文化美食夜市摆摊');

insert into activity (
    title, summary, detail, category, category_name, image_url, review_image_url, review_content, location,
    start_time, end_time, capacity, registered_count, status, required_role_code, creator_id, published_at
)
select
    '汪汪解压局',
    '在学业压力最大的时候，和可爱的小狗们共度时光，用最治愈的方式放松心情。',
    '在学业压力最大的时候，和可爱的小狗们共度时光，用最治愈的方式放松心情，重拾能量。',
    'welfare',
    '解压·治愈',
    'https://images.unsplash.com/photo-1548199973-03cce0bbc87b?auto=format&fit=crop&w=1200&q=80',
    null,
    null,
    'UNSW Main Library Lawn',
    '2026-07-03 13:00:00',
    '2026-07-03 16:00:00',
    220,
    76,
    'PUBLISHED',
    null,
    1,
    current_timestamp
where not exists (select 1 from activity where title = '汪汪解压局');

insert into activity (
    title, summary, detail, category, category_name, image_url, review_image_url, review_content, location,
    start_time, end_time, capacity, registered_count, status, required_role_code, creator_id, published_at
)
select
    '职规Networking & Peer Mentoring',
    '连接学长学姐与业界精英，提供职业规划指导、简历分享及企业招聘交流。',
    '连接学长学姐与业界精英，提供职业规划指导、简历撰写分享及企业招聘会，助力你的职业发展。',
    'career',
    '职业·学术',
    'https://images.unsplash.com/photo-1551836022-d5d88e9218df?auto=format&fit=crop&w=1200&q=80',
    null,
    null,
    'UNSW Business School Theatre',
    '2026-07-10 18:30:00',
    '2026-07-10 21:00:00',
    260,
    88,
    'PUBLISHED',
    null,
    1,
    current_timestamp
where not exists (select 1 from activity where title = '职规Networking & Peer Mentoring');

insert into activity (
    title, summary, detail, category, category_name, image_url, review_image_url, review_content, location,
    start_time, end_time, capacity, registered_count, status, required_role_code, creator_id, published_at
)
select
    '澳洲八大新生行前会',
    '由 UNSW 中国学联牵头发起，联合澳洲八大高校学联共同打造的八大联合国内行前项目。',
    '由UNSW中国学联牵头发起，联合澳洲八大高校学联共同打造的首个八大联合国内行前项目。在广州、上海、北京三城重磅落地，吸引超过500名新生及家长参与。',
    'major',
    '全澳首创',
    'https://images.unsplash.com/photo-1523240795612-9a054b0db644?auto=format&fit=crop&w=1200&q=80',
    'https://images.unsplash.com/photo-1523240795612-9a054b0db644?auto=format&fit=crop&w=1200&q=80',
    '广州、上海、北京三城落地，帮助新生和家长提前了解澳洲学习生活、入学准备和校园资源。',
    '广州 · 上海 · 北京',
    '2025-07-20 14:00:00',
    '2025-07-28 18:00:00',
    500,
    500,
    'ENDED',
    null,
    1,
    current_timestamp
where not exists (select 1 from activity where title = '澳洲八大新生行前会');

insert into activity (
    title, summary, detail, category, category_name, image_url, review_image_url, review_content, location,
    start_time, end_time, capacity, registered_count, status, required_role_code, creator_id, published_at
)
select
    '南半球官方电竞比赛',
    '联合腾讯官方合作落地举办，面向南半球所有学生开放，打造高规格电竞交流平台。',
    '联合腾讯官方合作落地举办，赛事涵盖《金铲铲之战》与《王者荣耀》两大热门项目。面向南半球所有学生开放，打造高规格电竞交流平台。',
    'major',
    '腾讯官方合作',
    'https://images.unsplash.com/photo-1542751371-adc38448a05e?auto=format&fit=crop&w=1200&q=80',
    'https://images.unsplash.com/photo-1542751371-adc38448a05e?auto=format&fit=crop&w=1200&q=80',
    '赛事覆盖金铲铲之战与王者荣耀，连接南半球学生玩家与官方赛事资源。',
    '线上赛事 · UNSW 线下观赛点',
    '2025-08-16 12:00:00',
    '2025-08-18 22:00:00',
    800,
    800,
    'ENDED',
    null,
    1,
    current_timestamp
where not exists (select 1 from activity where title = '南半球官方电竞比赛');

insert into activity (
    title, summary, detail, category, category_name, image_url, review_image_url, review_content, location,
    start_time, end_time, capacity, registered_count, status, required_role_code, creator_id, published_at
)
select
    '留学人员中秋国庆晚会',
    '2500余名在新州求学的留学生齐聚悉尼市政厅，共同点亮海外学子的中秋团圆夜。',
    '2025年9月27日，2500余名在新州求学的留学生齐聚悉尼市政厅，共同点亮这场属于海外学子的中秋团圆夜。中国驻悉尼总领馆共同出席见证。',
    'major',
    '2500+人',
    'https://images.unsplash.com/photo-1511795409834-ef04bbd61622?auto=format&fit=crop&w=1200&q=80',
    'https://images.unsplash.com/photo-1511795409834-ef04bbd61622?auto=format&fit=crop&w=1200&q=80',
    '悉尼市政厅中秋国庆晚会承载海外学子的团圆记忆，融合舞台表演、文化展示与社区连接。',
    '悉尼市政厅',
    '2025-09-27 18:30:00',
    '2025-09-27 22:30:00',
    2500,
    2500,
    'ENDED',
    null,
    1,
    current_timestamp
where not exists (select 1 from activity where title = '留学人员中秋国庆晚会');

insert into activity (
    title, summary, detail, category, category_name, image_url, review_image_url, review_content, location,
    start_time, end_time, capacity, registered_count, status, required_role_code, creator_id, published_at
)
select
    '「月下巡航」万圣节游轮派对',
    '在悉尼港湾璀璨夜景中举办万圣节游轮派对，500余名同学共度难忘一夜。',
    '在悉尼港湾璀璨夜景中举办万圣节游轮派对，500余名同学在Starship上共度难忘一夜，悉尼标志性风景成为最浪漫的舞台背景。',
    'major',
    '年终盛典',
    'https://images.unsplash.com/photo-1507525428034-b723cf961d3e?auto=format&fit=crop&w=1200&q=80',
    'https://images.unsplash.com/photo-1507525428034-b723cf961d3e?auto=format&fit=crop&w=1200&q=80',
    'Starship 游轮、悉尼港夜景与万圣节主题派对组成属于新南同学的年终盛典。',
    'Starship · Sydney Harbour',
    '2024-11-03 19:00:00',
    '2024-11-03 23:00:00',
    500,
    500,
    'ENDED',
    null,
    1,
    current_timestamp
where not exists (select 1 from activity where title = '「月下巡航」万圣节游轮派对');
