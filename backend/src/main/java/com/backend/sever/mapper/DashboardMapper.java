package com.backend.sever.mapper;

import com.backend.pojo.vo.ApiAccessLogVO;
import com.backend.pojo.vo.ApiTrafficPointVO;
import com.backend.pojo.vo.DashboardNameValueVO;
import com.backend.pojo.vo.DashboardRankVO;
import com.backend.pojo.vo.OperationLogVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DashboardMapper {
    long countUsers();

    long countMembers();


    long countDepartments();

    long countActivities();

    long countActivityRegistrations();

    long countCouponClaims();

    List<DashboardNameValueVO> selectActivityStatus();

    List<DashboardNameValueVO> selectMemberStatus();

    List<ApiTrafficPointVO> selectApiTraffic(@Param("minutes") int minutes);

    List<DashboardRankVO> selectHotApis(@Param("hours") int hours, @Param("size") int size);

    List<DashboardRankVO> selectSlowApis(@Param("hours") int hours, @Param("size") int size);

    List<DashboardRankVO> selectErrorApis(@Param("hours") int hours, @Param("size") int size);

    List<DashboardRankVO> selectActivityConversions(@Param("size") int size);

    List<DashboardRankVO> selectCouponConversions(@Param("size") int size);

    List<DashboardRankVO> selectActiveUsers(@Param("days") int days, @Param("size") int size);

    List<ApiAccessLogVO> selectRecentApiLogs(@Param("size") int size);

    List<OperationLogVO> selectRecentOperationLogs(@Param("size") int size);

    int insertApiAccessLog(
            @Param("method") String method,
            @Param("path") String path,
            @Param("statusCode") int statusCode,
            @Param("durationMs") long durationMs,
            @Param("userId") Long userId,
            @Param("username") String username,
            @Param("ipAddress") String ipAddress,
            @Param("userAgent") String userAgent
    );

    int insertOperationLog(
            @Param("userId") Long userId,
            @Param("username") String username,
            @Param("method") String method,
            @Param("path") String path,
            @Param("action") String action,
            @Param("statusCode") int statusCode,
            @Param("durationMs") long durationMs,
            @Param("ipAddress") String ipAddress
    );

    int refreshApiAccessMinuteStats(@Param("hours") int hours);

    int refreshApiPathHourStats(@Param("hours") int hours);

    int refreshUserActivityDayStats(@Param("days") int days);

    int deleteApiAccessMinuteStatsBefore(@Param("days") int days);

    int deleteApiPathHourStatsBefore(@Param("days") int days);

    int deleteUserActivityDayStatsBefore(@Param("days") int days);

    int deleteApiAccessLogsBefore(@Param("days") int days);

    int deleteOperationLogsBefore(@Param("days") int days);
}
