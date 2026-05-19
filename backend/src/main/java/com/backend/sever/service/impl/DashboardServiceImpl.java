package com.backend.sever.service.impl;

import com.backend.pojo.vo.DashboardMetricVO;
import com.backend.pojo.vo.DashboardOverviewVO;
import com.backend.pojo.vo.DashboardConversionVO;
import com.backend.pojo.vo.DashboardRankVO;
import com.backend.sever.mapper.DashboardMapper;
import com.backend.sever.service.DashboardService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class DashboardServiceImpl implements DashboardService {
    private static final String OVERVIEW_CACHE_KEY = "dashboard:overview:v1";

    private final DashboardMapper dashboardMapper;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public DashboardServiceImpl(
            DashboardMapper dashboardMapper,
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper
    ) {
        this.dashboardMapper = dashboardMapper;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public DashboardOverviewVO overview() {
        DashboardOverviewVO cached = readCache();
        if (cached != null) {
            fillRecentLogs(cached);
            return cached;
        }
        DashboardOverviewVO overview = new DashboardOverviewVO();
        overview.setRefreshedAt(LocalDateTime.now());
        overview.setMetrics(List.of(
                new DashboardMetricVO("用户总数", dashboardMapper.countUsers(), "cyan"),
                new DashboardMetricVO("成员总数", dashboardMapper.countMembers(), "green"),
                new DashboardMetricVO("部门数", dashboardMapper.countDepartments(), "violet"),
                new DashboardMetricVO("活动数", dashboardMapper.countActivities(), "orange"),
                new DashboardMetricVO("报名数", dashboardMapper.countActivityRegistrations(), "blue"),
                new DashboardMetricVO("优惠券领取数", dashboardMapper.countCouponClaims(), "pink")
        ));
        overview.setActivityStatus(dashboardMapper.selectActivityStatus());
        overview.setMemberStatus(dashboardMapper.selectMemberStatus());
        overview.setApiTraffic(dashboardMapper.selectApiTraffic(60));
        overview.setHotApis(dashboardMapper.selectHotApis(24, 8));
        overview.setSlowApis(dashboardMapper.selectSlowApis(24, 8));
        overview.setErrorApis(dashboardMapper.selectErrorApis(24, 8));
        overview.setActivityConversions(toConversions(dashboardMapper.selectActivityConversions(8)));
        overview.setCouponConversions(toConversions(dashboardMapper.selectCouponConversions(8)));
        overview.setActiveUsers(dashboardMapper.selectActiveUsers(7, 8));
        writeCache(overview);
        fillRecentLogs(overview);
        return overview;
    }

    private void fillRecentLogs(DashboardOverviewVO overview) {
        overview.setApiLogs(dashboardMapper.selectRecentApiLogs(20));
        overview.setOperationLogs(dashboardMapper.selectRecentOperationLogs(20));
    }

    private DashboardOverviewVO readCache() {
        try {
            String raw = redisTemplate.opsForValue().get(OVERVIEW_CACHE_KEY);
            if (raw == null || raw.isBlank()) {
                return null;
            }
            return objectMapper.readValue(raw, DashboardOverviewVO.class);
        } catch (RuntimeException | JsonProcessingException ignored) {
            return null;
        }
    }

    private void writeCache(DashboardOverviewVO overview) {
        try {
            redisTemplate.opsForValue().set(
                    OVERVIEW_CACHE_KEY,
                    objectMapper.writeValueAsString(overview),
                    45,
                    TimeUnit.SECONDS
            );
        } catch (RuntimeException | JsonProcessingException ignored) {
            // Redis is an optimization for the dashboard, not a hard dependency.
        }
    }

    private List<DashboardConversionVO> toConversions(List<DashboardRankVO> ranks) {
        return ranks.stream()
                .map(rank -> new DashboardConversionVO(
                        rank.getName(),
                        rank.getValue(),
                        parseTarget(rank.getDetail()),
                        rank.getRate() == null ? 0 : rank.getRate()
                ))
                .toList();
    }

    private long parseTarget(String detail) {
        if (detail == null || detail.isBlank()) {
            return 0;
        }
        try {
            return Long.parseLong(detail);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }
}
