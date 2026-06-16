create table if not exists restaurant (
    id bigint primary key auto_increment,
    name varchar(120) not null,
    address varchar(255) not null,
    latitude decimal(10, 7) not null,
    longitude decimal(10, 7) not null,
    category varchar(50) not null,
    price_level varchar(20) null,
    website_url varchar(500) null,
    cover_url varchar(500) null,
    status varchar(20) not null default 'ACTIVE',
    created_at datetime not null default current_timestamp,
    updated_at datetime not null default current_timestamp on update current_timestamp,
    key idx_restaurant_location (latitude, longitude),
    key idx_restaurant_category_status (category, status)
);

create table if not exists restaurant_review (
    id bigint primary key auto_increment,
    restaurant_id bigint not null,
    user_id bigint not null,
    rating tinyint not null,
    content varchar(1000) null,
    status varchar(20) not null default 'NORMAL',
    created_at datetime not null default current_timestamp,
    updated_at datetime not null default current_timestamp on update current_timestamp,
    unique key uk_restaurant_review_user (restaurant_id, user_id),
    key idx_restaurant_review_restaurant_time (restaurant_id, created_at),
    key idx_restaurant_review_user (user_id),
    constraint chk_restaurant_review_rating check (rating between 1 and 5)
);

create table if not exists restaurant_rating_stat (
    restaurant_id bigint primary key,
    rating_avg decimal(3, 2) not null default 0.00,
    review_count int not null default 0,
    rating_1_count int not null default 0,
    rating_2_count int not null default 0,
    rating_3_count int not null default 0,
    rating_4_count int not null default 0,
    rating_5_count int not null default 0,
    updated_at datetime not null default current_timestamp on update current_timestamp,
    key idx_restaurant_rating_rank (rating_avg, review_count)
);

alter table restaurant convert to character set utf8mb4 collate utf8mb4_unicode_ci;
alter table restaurant_review convert to character set utf8mb4 collate utf8mb4_unicode_ci;
alter table restaurant_rating_stat convert to character set utf8mb4 collate utf8mb4_unicode_ci;

insert ignore into restaurant (
    id, name, address, latitude, longitude, category, price_level, website_url, cover_url, status
) values
(1, 'Time for Thai Kensington', '2/309 Anzac Parade, Kingsford NSW 2032', -33.9215100, 151.2278900, 'thai', '$$', 'https://timeforthai.com.au', 'https://images.unsplash.com/photo-1559314809-0d155014e29e?auto=format&fit=crop&w=900&q=80', 'ACTIVE'),
(2, 'Mamak Village Kensington', 'Shop 3/391 Anzac Parade, Kingsford NSW 2032', -33.9231100, 151.2275100, 'malaysian', '$$', 'https://www.mamakvillage.com.au', 'https://images.unsplash.com/photo-1565299624946-b28f40a0ae38?auto=format&fit=crop&w=900&q=80', 'ACTIVE'),
(3, 'Ayam Goreng 99', '464 Anzac Parade, Kingsford NSW 2032', -33.9242400, 151.2270800, 'indonesian', '$$', 'https://ayamgoreng99.com.au', 'https://images.unsplash.com/photo-1604908176997-125f25cc6f3d?auto=format&fit=crop&w=900&q=80', 'ACTIVE'),
(4, 'The Sweet Spot Patisserie', '18 Perouse Rd, Randwick NSW 2031', -33.9203600, 151.2412800, 'dessert', '$$', 'https://thesweetspotpatisserie.com.au', 'https://images.unsplash.com/photo-1488477181946-6428a0291777?auto=format&fit=crop&w=900&q=80', 'ACTIVE'),
(5, 'Del Punto', '40 St Pauls St, Randwick NSW 2031', -33.9210100, 151.2417800, 'spanish', '$$$', 'https://delpunto.com.au', 'https://images.unsplash.com/photo-1515443961218-a51367888e4b?auto=format&fit=crop&w=900&q=80', 'ACTIVE'),
(6, 'Bar Lucio', '1/6 St Pauls St, Randwick NSW 2031', -33.9205700, 151.2410500, 'italian', '$$$', 'https://barlucio.com.au', 'https://images.unsplash.com/photo-1544148103-0773bf10d330?auto=format&fit=crop&w=900&q=80', 'ACTIVE'),
(7, 'Niji Sushi Bar', '333 Anzac Parade, Kingsford NSW 2032', -33.9220100, 151.2278000, 'japanese', '$$', 'https://niji.com.au', 'https://images.unsplash.com/photo-1579871494447-9811cf80d66c?auto=format&fit=crop&w=900&q=80', 'ACTIVE'),
(8, 'Cafe Jack''s Kensington', 'Kensington NSW 2033', -33.9178200, 151.2293800, 'cafe', '$$', 'https://example.com/cafe-jacks', 'https://images.unsplash.com/photo-1501339847302-ac426a4a7cbb?auto=format&fit=crop&w=900&q=80', 'ACTIVE');

insert ignore into restaurant_rating_stat (
    restaurant_id, rating_avg, review_count, rating_1_count, rating_2_count, rating_3_count, rating_4_count, rating_5_count
) values
(1, 4.60, 8, 0, 0, 1, 1, 6),
(2, 4.30, 5, 0, 0, 1, 2, 2),
(3, 4.70, 7, 0, 0, 0, 2, 5),
(4, 4.50, 4, 0, 0, 0, 2, 2),
(5, 4.20, 3, 0, 0, 1, 1, 1),
(6, 4.40, 4, 0, 0, 0, 2, 2),
(7, 4.10, 6, 0, 1, 1, 1, 3),
(8, 4.00, 2, 0, 0, 1, 0, 1);
