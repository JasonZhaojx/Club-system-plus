package com.backend.pojo.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class DashboardOverviewVO {
    private LocalDateTime refreshedAt;
    private List<DashboardMetricVO> metrics;
    private List<DashboardNameValueVO> activityStatus;
    private List<DashboardNameValueVO> memberStatus;
    private List<ApiTrafficPointVO> apiTraffic;
    private List<DashboardRankVO> hotApis;
    private List<DashboardRankVO> slowApis;
    private List<DashboardRankVO> errorApis;
    private List<DashboardConversionVO> activityConversions;
    private List<DashboardConversionVO> couponConversions;
    private List<DashboardRankVO> activeUsers;
    private List<ApiAccessLogVO> apiLogs;
    private List<OperationLogVO> operationLogs;
}
