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
