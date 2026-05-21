package com.backend.sever.service.impl;

import com.backend.sever.config.EmailCodeProperties;
import com.backend.sever.exception.BusinessException;
import com.backend.sever.exception.ErrorCode;
import com.backend.sever.service.EmailCodeService;
import com.backend.sever.service.MailService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class RedisEmailCodeService implements EmailCodeService {
    private static final String PASSWORD_RESET_KEY_PREFIX = "auth:email-code:password-reset:";
    private static final String FIELD_CODE_HASH = "codeHash";
    private static final String FIELD_FAIL_COUNT = "failCount";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final StringRedisTemplate redisTemplate;
    private final EmailCodeProperties properties;
    private final MailService mailService;

    public RedisEmailCodeService(
            StringRedisTemplate redisTemplate,
            EmailCodeProperties properties,
            MailService mailService
    ) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
        this.mailService = mailService;
    }

    @Override
    public void createAndSendPasswordResetCode(String email) {
        String normalizedEmail = normalizeEmail(email);
        String code = generateCode();
        redisTemplate.opsForHash().putAll(key(normalizedEmail), Map.of(
                FIELD_CODE_HASH, hashCode(normalizedEmail, code),
                FIELD_FAIL_COUNT, "0"
        ));
        redisTemplate.expire(key(normalizedEmail), Duration.ofSeconds(Math.max(properties.getTtlSeconds(), 60)));
        mailService.sendPasswordResetCode(normalizedEmail, code);
    }

    @Override
    public void verifyPasswordResetCode(String email, String code) {
        String normalizedEmail = normalizeEmail(email);
        if (!StringUtils.hasText(code)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "验证码不能为空");
        }
        String key = key(normalizedEmail);
        Object rawHash = redisTemplate.opsForHash().get(key, FIELD_CODE_HASH);
        if (rawHash == null) {
            throw new BusinessException(ErrorCode.CONFLICT, "验证码不存在或已过期");
        }
        int failCount = parseFailCount(redisTemplate.opsForHash().get(key, FIELD_FAIL_COUNT));
        if (failCount >= Math.max(properties.getMaxVerifyAttempts(), 1)) {
            redisTemplate.delete(key);
            throw new BusinessException(ErrorCode.CONFLICT, "验证码错误次数过多，请重新获取");
        }
        if (!constantTimeEquals(String.valueOf(rawHash), hashCode(normalizedEmail, code.trim()))) {
            redisTemplate.opsForHash().increment(key, FIELD_FAIL_COUNT, 1);
            throw new BusinessException(ErrorCode.CONFLICT, "验证码错误");
        }
        redisTemplate.delete(key);
    }

    private String normalizeEmail(String email) {
        if (!StringUtils.hasText(email)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "邮箱不能为空");
        }
        String normalized = email.trim().toLowerCase();
        if (normalized.length() > 120 || !normalized.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "邮箱格式不正确");
        }
        return normalized;
    }

    private String key(String email) {
        return PASSWORD_RESET_KEY_PREFIX + hash(email);
    }

    private String generateCode() {
        int value = SECURE_RANDOM.nextInt(1_000_000);
        return String.format("%06d", value);
    }

    private String hashCode(String email, String code) {
        return hash(email + ":" + code + ":" + properties.getSecret());
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to hash email code", exception);
        }
    }

    private int parseFailCount(Object value) {
        if (value == null) {
            return 0;
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private boolean constantTimeEquals(String expected, String actual) {
        byte[] expectedBytes = expected.getBytes(StandardCharsets.UTF_8);
        byte[] actualBytes = actual.getBytes(StandardCharsets.UTF_8);
        if (expectedBytes.length != actualBytes.length) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < expectedBytes.length; i++) {
            result |= expectedBytes[i] ^ actualBytes[i];
        }
        return result == 0;
    }
}
