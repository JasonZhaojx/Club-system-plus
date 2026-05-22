package com.backend.sever.service.impl;

import com.backend.sever.config.BusinessRateLimitProperties;
import com.backend.sever.exception.BusinessException;
import com.backend.sever.exception.ErrorCode;
import com.backend.sever.service.BusinessRateLimiter;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;

@Service
public class RedisBusinessRateLimiter implements BusinessRateLimiter {
    private static final DefaultRedisScript<Long> FIXED_WINDOW_SCRIPT = new DefaultRedisScript<>("""
            local current = redis.call('incr', KEYS[1])
            if current == 1 then
                redis.call('expire', KEYS[1], tonumber(ARGV[2]))
            end
            if current > tonumber(ARGV[1]) then
                return 0
            end
            return 1
            """, Long.class);

    private final StringRedisTemplate redisTemplate;
    private final BusinessRateLimitProperties properties;

    public RedisBusinessRateLimiter(StringRedisTemplate redisTemplate, BusinessRateLimitProperties properties) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
    }

    @Override
    public void checkLogin(String username, String ipAddress) {
        if (!properties.isEnabled()) {
            return;
        }
        String normalizedUsername = normalizeIdentity(username);
        if (StringUtils.hasText(normalizedUsername)) {
            requireAllowed(
                    "rate:biz:login:username:" + hash(normalizedUsername),
                    properties.getLoginUsernameLimit(),
                    properties.getLoginUsernameWindowSeconds(),
                    "鐧诲綍璇峰姹傝繃浜庨绻侊紝璇风◢鍚庡啀璇?"
            );
        }
        if (StringUtils.hasText(ipAddress)) {
            requireAllowed(
                    "rate:biz:login:ip:" + hash(ipAddress),
                    properties.getLoginIpLimit(),
                    properties.getLoginIpWindowSeconds(),
                    "褰撳墠缃戠粶鐧诲綍璇峰姹傝繃浜庨绻侊紝璇风◢鍚庡啀璇?"
            );
        }
    }

    @Override
    public void checkCouponClaim(Long userId, Long batchId) {
        if (!properties.isEnabled()) {
            return;
        }
        String key = "rate:biz:coupon:claim:" + userId + ":" + batchId;
        requireAllowed(key, properties.getCouponClaimLimit(), properties.getCouponClaimWindowSeconds(), "领券操作过于频繁，请稍后再试");
    }

    @Override
    public void checkActivityRegister(Long userId, Long activityId) {
        if (!properties.isEnabled()) {
            return;
        }
        String key = "rate:biz:activity:register:" + userId + ":" + activityId;
        requireAllowed(key, properties.getActivityRegisterLimit(), properties.getActivityRegisterWindowSeconds(), "报名操作过于频繁，请稍后再试");
    }

    @Override
    public void checkPasswordResetEmail(String email, String ipAddress) {
        if (!properties.isEnabled()) {
            return;
        }
        String emailHash = hash(normalizeEmail(email));
        requireAllowed(
                "rate:biz:email:password-reset:cooldown:" + emailHash,
                properties.getEmailCooldownLimit(),
                properties.getEmailCooldownWindowSeconds(),
                "验证码发送过于频繁，请稍后再试"
        );
        requireAllowed(
                "rate:biz:email:password-reset:hour:" + emailHash,
                properties.getEmailHourlyLimit(),
                properties.getEmailHourlyWindowSeconds(),
                "验证码发送次数过多，请稍后再试"
        );
        if (StringUtils.hasText(ipAddress)) {
            requireAllowed(
                    "rate:biz:email:password-reset:ip:" + hash(ipAddress),
                    properties.getEmailIpHourlyLimit(),
                    properties.getEmailIpHourlyWindowSeconds(),
                    "当前网络请求过于频繁，请稍后再试"
            );
        }
    }

    private void requireAllowed(String key, int limit, int windowSeconds, String message) {
        try {
            Long result = redisTemplate.execute(
                    FIXED_WINDOW_SCRIPT,
                    List.of(key),
                    String.valueOf(Math.max(limit, 1)),
                    String.valueOf(Math.max(windowSeconds, 1))
            );
            if (result != null && result == 0L) {
                throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS, message);
            }
        } catch (BusinessException exception) {
            throw exception;
        } catch (RuntimeException ignored) {
            // Redis is a protection layer. If it is unavailable, keep the business path available.
        }
    }

    private String normalizeEmail(String email) {
        return StringUtils.hasText(email) ? email.trim().toLowerCase() : "";
    }

    private String normalizeIdentity(String value) {
        return StringUtils.hasText(value) ? value.trim().toLowerCase() : "";
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to hash rate limit key", exception);
        }
    }
}
