package com.backend.sever.service.impl;

import com.backend.sever.mapper.DashboardMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DashboardStatScheduler {
    private static final String OVERVIEW_CACHE_KEY = "dashboard:overview:v1";

    private final DashboardMapper dashboardMapper;
    private final StringRedisTemplate redisTemplate;

    public DashboardStatScheduler(DashboardMapper dashboardMapper, StringRedisTemplate redisTemplate) {
        this.dashboardMapper = dashboardMapper;
        this.redisTemplate = redisTemplate;
    }

    @Scheduled(fixedDelay = 60_000, initialDelay = 15_000)
    public void refreshMinuteStats() {
        dashboardMapper.refreshApiAccessMinuteStats(2);
        clearOverviewCache();
    }

    @Scheduled(fixedDelay = 600_000, initialDelay = 30_000)
    public void refreshRankStats() {
        dashboardMapper.refreshApiPathHourStats(48);
        dashboardMapper.refreshUserActivityDayStats(7);
        clearOverviewCache();
    }

    @Scheduled(cron = "0 20 3 * * *")
    public void cleanupDashboardData() {
        dashboardMapper.refreshApiAccessMinuteStats(24);
        dashboardMapper.refreshApiPathHourStats(48);
        dashboardMapper.refreshUserActivityDayStats(7);
        dashboardMapper.deleteApiAccessLogsBefore(30);
        dashboardMapper.deleteOperationLogsBefore(180);
        dashboardMapper.deleteApiAccessMinuteStatsBefore(14);
        dashboardMapper.deleteApiPathHourStatsBefore(90);
        dashboardMapper.deleteUserActivityDayStatsBefore(180);
        clearOverviewCache();
    }

    private void clearOverviewCache() {
        try {
            redisTemplate.delete(OVERVIEW_CACHE_KEY);
        } catch (RuntimeException ignored) {
            // Redis only reduces dashboard load; scheduled aggregation should still complete without it.
        }
    }
}
