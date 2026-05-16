package com.backend.sever.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.backend.pojo.entity.ActivityRegistration;
import com.backend.pojo.entity.ActivityRegistrationStatus;
import com.backend.pojo.vo.ActivityRegistrationVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ActivityRegistrationMapper extends BaseMapper<ActivityRegistration> {
    ActivityRegistration selectByActivityAndUser(@Param("activityId") Long activityId, @Param("userId") Long userId);

    int reactivateRegistration(@Param("id") Long id);

    int updateStatus(@Param("id") Long id, @Param("status") ActivityRegistrationStatus status);

    List<ActivityRegistrationVO> selectUserRegistrations(@Param("userId") Long userId);
}
