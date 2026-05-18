package com.backend.sever.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.backend.pojo.entity.Activity;
import com.backend.pojo.entity.ActivityStatus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ActivityMapper extends BaseMapper<Activity> {
    List<Activity> selectActivityPage(
            @Param("keyword") String keyword,
            @Param("category") String category,
            @Param("status") ActivityStatus status,
            @Param("publicOnly") boolean publicOnly,
            @Param("sort") String sort,
            @Param("offset") int offset,
            @Param("size") int size
    );

    long countActivities(
            @Param("keyword") String keyword,
            @Param("category") String category,
            @Param("status") ActivityStatus status,
            @Param("publicOnly") boolean publicOnly
    );

    int updateActivity(Activity activity);

    int updateStatus(
            @Param("id") Long id,
            @Param("status") ActivityStatus status,
            @Param("reviewerId") Long reviewerId
    );

    int incrementRegistrationCount(@Param("id") Long id);

    int decrementRegistrationCount(@Param("id") Long id);
}
