package com.backend.sever.service.impl;

import com.backend.common.auth.UserPrincipal;
import com.backend.sever.exception.BusinessException;
import com.backend.sever.exception.ErrorCode;
import com.backend.sever.service.AssistantUsageLimitService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

@Service
public class AssistantUsageLimitServiceImpl implements AssistantUsageLimitService {
    private static final String KEY_PREFIX = "assistant:usage:";
    private static final int REGISTERED_LIMIT = 10;
    private static final int MEMBER_LIMIT = 30;

    private final StringRedisTemplate redisTemplate;

    public AssistantUsageLimitServiceImpl(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void checkAndConsume(UserPrincipal principal, String clientIp) {
        UsagePolicy policy = resolvePolicy(principal);
        if (policy.unlimited()) {
            return;
        }

        String key = KEY_PREFIX + LocalDate.now() + ":user:" + principal.userId();
        try {
            Long used = redisTemplate.opsForValue().increment(key);
            if (used != null && used == 1L) {
                redisTemplate.expire(key, Duration.ofDays(2));
            }
            if (used != null && used > policy.limit()) {
                throw new BusinessException(
                        ErrorCode.TOO_MANY_REQUESTS,
                        policy.reason() + " Daily AI assistant limit is " + policy.limit() + " messages."
                );
            }
        } catch (BusinessException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "AI usage quota check failed");
        }
    }

    private UsagePolicy resolvePolicy(UserPrincipal principal) {
        if (principal == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Please login before using AI assistant");
        }
        if ("root".equalsIgnoreCase(principal.username()) || hasAnyRole(
                principal.roles(),
                "SYSTEM_MAINTAINER",
                "PRESIDENT",
                "DEPARTMENT_LEADER"
        )) {
            return new UsagePolicy(0, true, "");
        }
        if (principal.roles().contains("CLUB_MEMBER")) {
            return new UsagePolicy(MEMBER_LIMIT, false, "Member quota exceeded.");
        }
        return new UsagePolicy(REGISTERED_LIMIT, false, "Registered user quota exceeded.");
    }

    private boolean hasAnyRole(List<String> roles, String... requiredRoles) {
        for (String requiredRole : requiredRoles) {
            if (roles.contains(requiredRole)) {
                return true;
            }
        }
        return false;
    }

    private record UsagePolicy(int limit, boolean unlimited, String reason) {
    }
}
