package com.backend.sever.service;

import com.backend.common.auth.UserPrincipal;
import com.backend.pojo.dto.ActivityCreateDTO;
import com.backend.pojo.dto.ActivityUpdateDTO;
import com.backend.pojo.entity.ActivityStatus;
import com.backend.pojo.vo.ActivityRegistrationVO;
import com.backend.pojo.vo.ActivityVO;
import com.backend.pojo.vo.PageVO;

import java.util.List;

public interface ActivityService {
    PageVO<ActivityVO> listPublicActivities(String keyword, String category, String sort, int page, int size);

    PageVO<ActivityVO> listManageActivities(String keyword, String category, ActivityStatus status, String sort, int page, int size);

    ActivityVO getPublicActivity(Long activityId);

    ActivityVO getActivity(Long activityId);

    ActivityVO createActivity(UserPrincipal principal, ActivityCreateDTO request);

    ActivityVO updateActivity(Long activityId, ActivityUpdateDTO request);

    ActivityVO submitReview(Long activityId);

    ActivityVO publishActivity(UserPrincipal principal, Long activityId);

    ActivityVO cancelActivity(Long activityId);

    ActivityVO finishActivity(Long activityId);

    ActivityRegistrationVO registerActivity(UserPrincipal principal, Long activityId);

    void cancelRegistration(UserPrincipal principal, Long activityId);

    List<ActivityRegistrationVO> listMyRegistrations(UserPrincipal principal);
}
