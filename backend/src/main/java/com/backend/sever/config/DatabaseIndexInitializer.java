package com.backend.sever.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseIndexInitializer implements ApplicationRunner {
    private static final String DEFAULT_AVATAR_URL = "https://ts1.tc.mm.bing.net/th/id/OIP-C.4n3KcdpOWTC32-U0LjDagwHaHa?cb=thfc1falcon&rs=1&pid=ImgDetMain&o=7&rm=3";

    private final JdbcTemplate jdbcTemplate;

    public DatabaseIndexInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        addColumn("app_user", "avatar_url",
                "alter table app_user add column avatar_url varchar(500) not null default '" + DEFAULT_AVATAR_URL + "' after email");
        addColumn("app_user", "token_version",
                "alter table app_user add column token_version int not null default 0 after status");
        jdbcTemplate.update("update app_user set avatar_url = ? where avatar_url is null or avatar_url = ''", DEFAULT_AVATAR_URL);
        addIndex("app_user", "idx_app_user_email",
                "alter table app_user add index idx_app_user_email (email)");
        addIndex("club_member", "idx_club_member_department_status",
                "alter table club_member add index idx_club_member_department_status (department_id, status)");
        addIndex("activity", "idx_activity_public_query",
                "alter table activity add index idx_activity_public_query (status, category, start_time, id)");
        addIndex("activity", "idx_activity_latest_query",
                "alter table activity add index idx_activity_latest_query (status, published_at, id)");
        addIndex("activity", "idx_activity_capacity_query",
                "alter table activity add index idx_activity_capacity_query (status, registered_count, capacity)");
        addIndex("activity_registration", "idx_activity_registration_activity_status",
                "alter table activity_registration add index idx_activity_registration_activity_status (activity_id, status)");
        addIndex("coupon_batch", "idx_coupon_batch_status_created",
                "alter table coupon_batch add index idx_coupon_batch_status_created (status, created_at, id)");
        addIndex("coupon_batch", "idx_coupon_batch_active_window",
                "alter table coupon_batch add index idx_coupon_batch_active_window (status, claim_start_time, claim_end_time, expire_time)");
        addIndex("user_coupon", "idx_user_coupon_user_status",
                "alter table user_coupon add index idx_user_coupon_user_status (user_id, status)");
        addIndex("user_coupon", "idx_user_coupon_batch_status",
                "alter table user_coupon add index idx_user_coupon_batch_status (batch_id, status)");
    }

    private void addColumn(String tableName, String columnName, String ddl) {
        Integer count = jdbcTemplate.queryForObject(
                """
                        select count(1)
                        from information_schema.columns
                        where table_schema = database()
                          and table_name = ?
                          and column_name = ?
                        """,
                Integer.class,
                tableName,
                columnName
        );
        if (count == null || count == 0) {
            jdbcTemplate.execute(ddl);
        }
    }

    private void addIndex(String tableName, String indexName, String ddl) {
        Integer count = jdbcTemplate.queryForObject(
                """
                        select count(1)
                        from information_schema.statistics
                        where table_schema = database()
                          and table_name = ?
                          and index_name = ?
                        """,
                Integer.class,
                tableName,
                indexName
        );
        if (count == null || count == 0) {
            jdbcTemplate.execute(ddl);
        }
    }
}
