package com.backend.sever.controller;

import com.backend.common.auth.UserPrincipal;
import com.backend.pojo.dto.ActivityCreateDTO;
import com.backend.pojo.dto.ActivityReviewUpdateDTO;
import com.backend.pojo.dto.ActivityUpdateDTO;
import com.backend.pojo.entity.ActivityStatus;
import com.backend.pojo.vo.ActivityRegistrationVO;
import com.backend.pojo.vo.ActivityVO;
import com.backend.pojo.vo.PageVO;
import com.backend.sever.common.Result;
import com.backend.sever.exception.BusinessException;
import com.backend.sever.exception.ErrorCode;
import com.backend.sever.service.ActivityService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/activities")
public class ActivityController {
    private final ActivityService activityService;

    public ActivityController(ActivityService activityService) {
        this.activityService = activityService;
    }

    @GetMapping
    public Result<PageVO<ActivityVO>> listPublicActivities(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) ActivityStatus status,
            @RequestParam(defaultValue = "upcoming") String sort,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return Result.success(activityService.listPublicActivities(keyword, category, status, sort, page, size));
    }

    @GetMapping("/{activityId}")
    public Result<ActivityVO> getPublicActivity(@PathVariable Long activityId) {
        return Result.success(activityService.getPublicActivity(activityId));
    }

    @GetMapping("/manage")
    @PreAuthorize("hasAnyAuthority('activity:create', 'activity:update', 'activity:review', 'system:maintain')")
    public Result<PageVO<ActivityVO>> listManageActivities(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) ActivityStatus status,
            @RequestParam(defaultValue = "latest") String sort,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return Result.success(activityService.listManageActivities(keyword, category, status, sort, page, size));
    }

    @GetMapping("/manage/{activityId}")
    @PreAuthorize("hasAnyAuthority('activity:create', 'activity:update', 'activity:review', 'system:maintain')")
    public Result<ActivityVO> getManageActivity(@PathVariable Long activityId) {
        return Result.success(activityService.getActivity(activityId));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('activity:create', 'system:maintain')")
    public Result<ActivityVO> createActivity(Authentication authentication, @RequestBody ActivityCreateDTO request) {
        return Result.success(activityService.createActivity(currentPrincipal(authentication), request));
    }

    @PutMapping("/{activityId}")
    @PreAuthorize("hasAnyAuthority('activity:update', 'system:maintain')")
    public Result<ActivityVO> updateActivity(@PathVariable Long activityId, @RequestBody ActivityUpdateDTO request) {
        return Result.success(activityService.updateActivity(activityId, request));
    }

    @PatchMapping("/{activityId}/review")
    @PreAuthorize("hasAnyAuthority('activity:review', 'system:maintain')")
    public Result<ActivityVO> updateActivityReview(
            @PathVariable Long activityId,
            @RequestBody ActivityReviewUpdateDTO request
    ) {
        return Result.success(activityService.updateActivityReview(activityId, request));
    }

    @PatchMapping("/{activityId}/submit")
    @PreAuthorize("hasAnyAuthority('activity:create', 'activity:update', 'system:maintain')")
    public Result<ActivityVO> submitReview(@PathVariable Long activityId) {
        return Result.success(activityService.submitReview(activityId));
    }

    @PatchMapping("/{activityId}/publish")
    @PreAuthorize("hasAnyAuthority('activity:review', 'system:maintain')")
    public Result<ActivityVO> publishActivity(Authentication authentication, @PathVariable Long activityId) {
        return Result.success(activityService.publishActivity(currentPrincipal(authentication), activityId));
    }

    @PatchMapping("/{activityId}/cancel")
    @PreAuthorize("hasAnyAuthority('activity:cancel', 'activity:review', 'system:maintain')")
    public Result<ActivityVO> cancelActivity(@PathVariable Long activityId) {
        return Result.success(activityService.cancelActivity(activityId));
    }

    @PatchMapping("/{activityId}/finish")
    @PreAuthorize("hasAnyAuthority('activity:review', 'system:maintain')")
    public Result<ActivityVO> finishActivity(@PathVariable Long activityId) {
        return Result.success(activityService.finishActivity(activityId));
    }

    @PostMapping("/{activityId}/registrations")
    public Result<ActivityRegistrationVO> registerActivity(Authentication authentication, @PathVariable Long activityId) {
        return Result.success(activityService.registerActivity(currentPrincipal(authentication), activityId));
    }

    @DeleteMapping("/{activityId}/registrations")
    public Result<Void> cancelRegistration(Authentication authentication, @PathVariable Long activityId) {
        activityService.cancelRegistration(currentPrincipal(authentication), activityId);
        return Result.success();
    }

    @GetMapping("/registrations/me")
    public Result<List<ActivityRegistrationVO>> listMyRegistrations(Authentication authentication) {
        return Result.success(activityService.listMyRegistrations(currentPrincipal(authentication)));
    }

    private UserPrincipal currentPrincipal(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return principal;
    }
}
