package com.backend.sever.service.impl;

import com.backend.common.auth.UserPrincipal;
import com.backend.pojo.dto.ActivityCreateDTO;
import com.backend.pojo.dto.ActivityUpdateDTO;
import com.backend.pojo.entity.Activity;
import com.backend.pojo.entity.ActivityRegistration;
import com.backend.pojo.entity.ActivityRegistrationStatus;
import com.backend.pojo.entity.ActivityStatus;
import com.backend.pojo.vo.ActivityRegistrationVO;
import com.backend.pojo.vo.ActivityVO;
import com.backend.pojo.vo.PageVO;
import com.backend.sever.exception.BusinessException;
import com.backend.sever.exception.ErrorCode;
import com.backend.sever.mapper.ActivityMapper;
import com.backend.sever.mapper.ActivityRegistrationMapper;
import com.backend.sever.service.BusinessRateLimiter;
import com.backend.sever.service.SentinelGuard;
import com.backend.sever.config.SentinelResourceNames;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
public class ActivityServiceImpl implements com.backend.sever.service.ActivityService {
    private static final String PUBLIC_LIST_CACHE_PREFIX = "hot:activity:list:";
    private static final String PUBLIC_DETAIL_CACHE_PREFIX = "hot:activity:detail:";
    private static final TypeReference<PageVO<ActivityVO>> ACTIVITY_PAGE_TYPE = new TypeReference<>() {
    };

    private final ActivityMapper activityMapper;
    private final ActivityRegistrationMapper activityRegistrationMapper;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final BusinessRateLimiter businessRateLimiter;
    private final SentinelGuard sentinelGuard;
    private final Cache<String, PageVO<ActivityVO>> publicListLocalCache;
    private final Cache<Long, ActivityVO> publicDetailLocalCache;

    public ActivityServiceImpl(
            ActivityMapper activityMapper,
            ActivityRegistrationMapper activityRegistrationMapper,
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            BusinessRateLimiter businessRateLimiter,
            SentinelGuard sentinelGuard
    ) {
        this.activityMapper = activityMapper;
        this.activityRegistrationMapper = activityRegistrationMapper;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.businessRateLimiter = businessRateLimiter;
        this.sentinelGuard = sentinelGuard;
        this.publicListLocalCache = Caffeine.newBuilder()
                .maximumSize(1_000)
                .expireAfterWrite(Duration.ofSeconds(20))
                .build();
        this.publicDetailLocalCache = Caffeine.newBuilder()
                .maximumSize(5_000)
                .expireAfterWrite(Duration.ofSeconds(45))
                .build();
    }

    @Override
    public PageVO<ActivityVO> listPublicActivities(String keyword, String category, String sort, int page, int size) {
        String cacheKey = PUBLIC_LIST_CACHE_PREFIX + cachePart(keyword) + ":" + cachePart(category) + ":" + cachePart(sort) + ":" + page + ":" + size;
        PageVO<ActivityVO> localCached = publicListLocalCache.getIfPresent(cacheKey);
        if (localCached != null) {
            return localCached;
        }
        PageVO<ActivityVO> cached = readCache(cacheKey, ACTIVITY_PAGE_TYPE);
        if (cached != null) {
            publicListLocalCache.put(cacheKey, cached);
            return cached;
        }
        PageVO<ActivityVO> result = listActivities(keyword, category, null, true, sort, page, size);
        writeCache(cacheKey, result, 30);
        publicListLocalCache.put(cacheKey, result);
        return result;
    }

    @Override
    public PageVO<ActivityVO> listManageActivities(String keyword, String category, ActivityStatus status, String sort, int page, int size) {
        return listActivities(keyword, category, status, false, sort, page, size);
    }

    @Override
    public ActivityVO getPublicActivity(Long activityId) {
        String cacheKey = PUBLIC_DETAIL_CACHE_PREFIX + activityId;
        ActivityVO localCached = publicDetailLocalCache.getIfPresent(activityId);
        if (localCached != null) {
            return localCached;
        }
        ActivityVO cached = readCache(cacheKey, ActivityVO.class);
        if (cached != null) {
            publicDetailLocalCache.put(activityId, cached);
            return cached;
        }
        Activity activity = requireActivity(activityId);
        if (activity.getStatus() != ActivityStatus.PUBLISHED && activity.getStatus() != ActivityStatus.ENDED) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "活动不存在或未发布");
        }
        ActivityVO result = ActivityVO.from(activity);
        writeCache(cacheKey, result, 60);
        publicDetailLocalCache.put(activityId, result);
        return result;
    }

    @Override
    public ActivityVO getActivity(Long activityId) {
        return ActivityVO.from(requireActivity(activityId));
    }

    @Override
    @Transactional
    public ActivityVO createActivity(UserPrincipal principal, ActivityCreateDTO request) {
        Activity activity = new Activity();
        fillActivity(activity, request);
        activity.setStatus(ActivityStatus.DRAFT);
        activity.setRegisteredCount(0);
        activity.setCreatorId(principal.userId());
        activityMapper.insert(activity);
        evictPublicActivityCaches(null);
        return ActivityVO.from(activityMapper.selectById(activity.getId()));
    }

    @Override
    @Transactional
    public ActivityVO updateActivity(Long activityId, ActivityUpdateDTO request) {
        Activity activity = requireActivity(activityId);
        if (activity.getStatus() == ActivityStatus.CANCELLED || activity.getStatus() == ActivityStatus.ENDED) {
            throw new BusinessException(ErrorCode.CONFLICT, "已取消或已结束的活动不能修改");
        }
        if (request == null || request.getCapacity() == null || request.getCapacity() < activity.getRegisteredCount()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "活动容量不能小于已报名人数");
        }
        fillActivity(activity, request);
        activityMapper.updateActivity(activity);
        evictPublicActivityCaches(activityId);
        return ActivityVO.from(activityMapper.selectById(activityId));
    }

    @Override
    @Transactional
    public ActivityVO submitReview(Long activityId) {
        Activity activity = requireActivity(activityId);
        if (activity.getStatus() != ActivityStatus.DRAFT) {
            throw new BusinessException(ErrorCode.CONFLICT, "只有草稿活动可以提交审核");
        }
        activityMapper.updateStatus(activityId, ActivityStatus.PENDING_REVIEW, null);
        evictPublicActivityCaches(activityId);
        return ActivityVO.from(activityMapper.selectById(activityId));
    }

    @Override
    @Transactional
    public ActivityVO publishActivity(UserPrincipal principal, Long activityId) {
        Activity activity = requireActivity(activityId);
        if (activity.getStatus() != ActivityStatus.PENDING_REVIEW && activity.getStatus() != ActivityStatus.DRAFT) {
            throw new BusinessException(ErrorCode.CONFLICT, "当前状态不能发布");
        }
        activityMapper.updateStatus(activityId, ActivityStatus.PUBLISHED, principal.userId());
        evictPublicActivityCaches(activityId);
        return ActivityVO.from(activityMapper.selectById(activityId));
    }

    @Override
    @Transactional
    public ActivityVO cancelActivity(Long activityId) {
        Activity activity = requireActivity(activityId);
        if (activity.getStatus() == ActivityStatus.ENDED) {
            throw new BusinessException(ErrorCode.CONFLICT, "已结束活动不能取消");
        }
        activityMapper.updateStatus(activityId, ActivityStatus.CANCELLED, null);
        evictPublicActivityCaches(activityId);
        return ActivityVO.from(activityMapper.selectById(activityId));
    }

    @Override
    @Transactional
    public ActivityVO finishActivity(Long activityId) {
        Activity activity = requireActivity(activityId);
        if (activity.getStatus() != ActivityStatus.PUBLISHED) {
            throw new BusinessException(ErrorCode.CONFLICT, "只有已发布活动可以结束");
        }
        activityMapper.updateStatus(activityId, ActivityStatus.ENDED, null);
        evictPublicActivityCaches(activityId);
        return ActivityVO.from(activityMapper.selectById(activityId));
    }

    @Override
    @Transactional
    public ActivityRegistrationVO registerActivity(UserPrincipal principal, Long activityId) {
        businessRateLimiter.checkActivityRegister(principal.userId(), activityId);
        try (SentinelGuard.GuardEntry guard = sentinelGuard.enter(SentinelResourceNames.ACTIVITY_REGISTER, activityId)) {
            try {
                return doRegisterActivity(principal, activityId);
            } catch (RuntimeException exception) {
                guard.trace(exception);
                throw exception;
            }
        }
    }

    private ActivityRegistrationVO doRegisterActivity(UserPrincipal principal, Long activityId) {
        Activity activity = requireActivity(activityId);
        if (activity.getStatus() != ActivityStatus.PUBLISHED) {
            throw new BusinessException(ErrorCode.CONFLICT, "只有已发布活动可以报名");
        }
        if (!LocalDateTime.now().isBefore(activity.getStartTime())) {
            throw new BusinessException(ErrorCode.CONFLICT, "活动已开始，不能继续报名");
        }
        if (activity.getRequiredRoleCode() != null && !roleSatisfies(principal.roles(), activity.getRequiredRoleCode())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "当前身份不符合活动报名要求");
        }
        ActivityRegistration existing = activityRegistrationMapper.selectByActivityAndUser(activityId, principal.userId());
        if (existing != null && existing.getStatus() == ActivityRegistrationStatus.REGISTERED) {
            throw new BusinessException(ErrorCode.CONFLICT, "不能重复报名");
        }
        if (activityMapper.incrementRegistrationCount(activityId) == 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "活动名额已满");
        }
        if (existing == null) {
            ActivityRegistration registration = new ActivityRegistration();
            registration.setActivityId(activityId);
            registration.setUserId(principal.userId());
            registration.setStatus(ActivityRegistrationStatus.REGISTERED);
            activityRegistrationMapper.insert(registration);
        } else {
            activityRegistrationMapper.reactivateRegistration(existing.getId());
        }
        evictPublicActivityCaches(activityId);
        return activityRegistrationMapper.selectUserRegistrations(principal.userId())
                .stream()
                .filter(item -> item.getActivityId().equals(activityId))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_ERROR, "报名记录生成失败"));
    }

    @Override
    @Transactional
    public void cancelRegistration(UserPrincipal principal, Long activityId) {
        ActivityRegistration registration = activityRegistrationMapper.selectByActivityAndUser(activityId, principal.userId());
        if (registration == null || registration.getStatus() != ActivityRegistrationStatus.REGISTERED) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "报名记录不存在");
        }
        activityRegistrationMapper.updateStatus(registration.getId(), ActivityRegistrationStatus.CANCELLED);
        activityMapper.decrementRegistrationCount(activityId);
        evictPublicActivityCaches(activityId);
    }

    @Override
    public List<ActivityRegistrationVO> listMyRegistrations(UserPrincipal principal) {
        return activityRegistrationMapper.selectUserRegistrations(principal.userId());
    }

    private PageVO<ActivityVO> listActivities(String keyword, String category, ActivityStatus status, boolean publicOnly, String sort, int page, int size) {
        int normalizedPage = Math.max(page, 1);
        int normalizedSize = Math.min(Math.max(size, 1), 50);
        int offset = (normalizedPage - 1) * normalizedSize;
        String normalizedKeyword = StringUtils.hasText(keyword) ? keyword.trim() : null;
        String normalizedCategory = StringUtils.hasText(category) ? category.trim() : null;
        String normalizedSort = StringUtils.hasText(sort) ? sort.trim() : "upcoming";
        return new PageVO<>(
                activityMapper.selectActivityPage(normalizedKeyword, normalizedCategory, status, publicOnly, normalizedSort, offset, normalizedSize)
                        .stream()
                        .map(ActivityVO::from)
                        .toList(),
                activityMapper.countActivities(normalizedKeyword, normalizedCategory, status, publicOnly),
                normalizedPage,
                normalizedSize
        );
    }

    private Activity requireActivity(Long activityId) {
        if (activityId == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "活动 ID 不能为空");
        }
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "活动不存在");
        }
        return activity;
    }

    private void fillActivity(Activity activity, ActivityCreateDTO request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "活动信息不能为空");
        }
        String title = requireText(request.getTitle(), "活动标题不能为空", 120);
        String summary = requireText(request.getSummary(), "活动摘要不能为空", 255);
        String detail = requireText(request.getDetail(), "活动详情不能为空", 4000);
        String category = requireText(request.getCategory(), "活动分类不能为空", 50);
        String categoryName = requireText(request.getCategoryName(), "活动分类名称不能为空", 50);
        String location = requireText(request.getLocation(), "活动地点不能为空", 120);
        if (request.getStartTime() == null || request.getEndTime() == null || !request.getEndTime().isAfter(request.getStartTime())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "活动时间范围不正确");
        }
        if (request.getCapacity() == null || request.getCapacity() < 1 || request.getCapacity() > 100000) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "活动名额必须在 1 到 100000 之间");
        }
        activity.setTitle(title);
        activity.setSummary(summary);
        activity.setDetail(detail);
        activity.setCategory(category);
        activity.setCategoryName(categoryName);
        activity.setImageUrl(normalizeOptional(request.getImageUrl(), 500));
        activity.setLocation(location);
        activity.setStartTime(request.getStartTime());
        activity.setEndTime(request.getEndTime());
        activity.setCapacity(request.getCapacity());
        activity.setRequiredRoleCode(normalizeOptional(request.getRequiredRoleCode(), 50));
    }

    private void fillActivity(Activity activity, ActivityUpdateDTO request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "活动信息不能为空");
        }
        String title = requireText(request.getTitle(), "活动标题不能为空", 120);
        String summary = requireText(request.getSummary(), "活动摘要不能为空", 255);
        String detail = requireText(request.getDetail(), "活动详情不能为空", 4000);
        String category = requireText(request.getCategory(), "活动分类不能为空", 50);
        String categoryName = requireText(request.getCategoryName(), "活动分类名称不能为空", 50);
        String location = requireText(request.getLocation(), "活动地点不能为空", 120);
        if (request.getStartTime() == null || request.getEndTime() == null || !request.getEndTime().isAfter(request.getStartTime())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "活动时间范围不正确");
        }
        if (request.getCapacity() == null || request.getCapacity() < 1 || request.getCapacity() > 100000) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "活动名额必须在 1 到 100000 之间");
        }
        activity.setTitle(title);
        activity.setSummary(summary);
        activity.setDetail(detail);
        activity.setCategory(category);
        activity.setCategoryName(categoryName);
        activity.setImageUrl(normalizeOptional(request.getImageUrl(), 500));
        activity.setLocation(location);
        activity.setStartTime(request.getStartTime());
        activity.setEndTime(request.getEndTime());
        activity.setCapacity(request.getCapacity());
        activity.setRequiredRoleCode(normalizeOptional(request.getRequiredRoleCode(), 50));
    }

    private String requireText(String value, String message, int maxLength) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, message);
        }
        String trimmed = value.trim();
        if (trimmed.length() > maxLength) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "字段长度不能超过 " + maxLength + " 个字符");
        }
        return trimmed;
    }

    private String normalizeOptional(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() > maxLength) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "字段长度不能超过 " + maxLength + " 个字符");
        }
        return trimmed;
    }

    private boolean roleSatisfies(List<String> roles, String requiredRoleCode) {
        if (roles.contains("SYSTEM_MAINTAINER")) {
            return true;
        }
        return switch (requiredRoleCode) {
            case "REGISTERED_USER" -> !roles.isEmpty();
            case "CLUB_MEMBER" -> roles.contains("CLUB_MEMBER")
                    || roles.contains("DEPARTMENT_LEADER")
                    || roles.contains("PRESIDENT");
            case "DEPARTMENT_LEADER" -> roles.contains("DEPARTMENT_LEADER") || roles.contains("PRESIDENT");
            case "PRESIDENT" -> roles.contains("PRESIDENT");
            default -> roles.contains(requiredRoleCode);
        };
    }

    private String cachePart(String value) {
        return StringUtils.hasText(value) ? value.trim() : "_";
    }

    private <T> T readCache(String key, Class<T> type) {
        try {
            String raw = redisTemplate.opsForValue().get(key);
            return raw == null ? null : objectMapper.readValue(raw, type);
        } catch (RuntimeException | JsonProcessingException ignored) {
            return null;
        }
    }

    private <T> T readCache(String key, TypeReference<T> type) {
        try {
            String raw = redisTemplate.opsForValue().get(key);
            return raw == null ? null : objectMapper.readValue(raw, type);
        } catch (RuntimeException | JsonProcessingException ignored) {
            return null;
        }
    }

    private void writeCache(String key, Object value, long ttlSeconds) {
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(value), ttlSeconds, TimeUnit.SECONDS);
        } catch (RuntimeException | JsonProcessingException ignored) {
            // Hot cache is an optimization; database remains the source of truth
        }
    }

    private void evictPublicActivityCaches(Long activityId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            doEvictPublicActivityCaches(activityId);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                doEvictPublicActivityCaches(activityId);
            }
        });
    }

    private void doEvictPublicActivityCaches(Long activityId) {
        publicListLocalCache.invalidateAll();
        if (activityId != null) {
            publicDetailLocalCache.invalidate(activityId);
        }
        try {
            Set<String> keys = redisTemplate.keys(PUBLIC_LIST_CACHE_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
            if (activityId != null) {
                redisTemplate.delete(PUBLIC_DETAIL_CACHE_PREFIX + activityId);
            }
        } catch (RuntimeException ignored) {
            // Cache invalidation failure should not break business writes.
        }
    }
}
