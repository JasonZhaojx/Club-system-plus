package com.backend.sever.service.impl;

import com.backend.pojo.entity.CouponBatch;
import com.backend.sever.service.CouponSeckillService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class RedisCouponSeckillService implements CouponSeckillService {
    public static final String CLAIM_QUEUE_KEY = "coupon:claim:queue";
    private static final String STOCK_KEY_PREFIX = "coupon:batch:";
    private static final String STOCK_KEY_SUFFIX = ":stock";
    private static final String USER_KEY_SUFFIX = ":users";
    private static final long COUPON_REDIS_RETENTION_DAYS = 5;
    private static final long MIN_TTL_SECONDS = 60;

    private static final String CLAIM_SCRIPT = """
            local ttl = tonumber(ARGV[3])
            if redis.call('sismember', KEYS[2], ARGV[1]) == 1 then
                redis.call('expire', KEYS[1], ttl)
                redis.call('expire', KEYS[2], ttl)
                return 2
            end
            local stock = tonumber(redis.call('get', KEYS[1]) or '-1')
            if stock <= 0 then
                redis.call('expire', KEYS[1], ttl)
                redis.call('expire', KEYS[2], ttl)
                return 1
            end
            redis.call('decr', KEYS[1])
            redis.call('sadd', KEYS[2], ARGV[1])
            redis.call('rpush', KEYS[3], ARGV[2])
            redis.call('expire', KEYS[1], ttl)
            redis.call('expire', KEYS[2], ttl)
            return 0
            """;

    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<Long> claimScript;

    public RedisCouponSeckillService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.claimScript = new DefaultRedisScript<>(CLAIM_SCRIPT, Long.class);
    }

    @Override
    public void preloadCoupon(CouponBatch batch) {
        int remaining = Math.max(batch.getStock() - batch.getClaimedCount(), 0);
        long ttlSeconds = ttlSeconds(batch);
        redisTemplate.opsForValue().set(stockKey(batch.getId()), String.valueOf(remaining), ttlSeconds, TimeUnit.SECONDS);
        redisTemplate.expire(userKey(batch.getId()), ttlSeconds, TimeUnit.SECONDS);
    }

    @Override
    public ClaimResult tryClaim(CouponBatch batch, Long userId) {
        String payload = batch.getId() + ":" + userId;
        Long result = redisTemplate.execute(
                claimScript,
                List.of(stockKey(batch.getId()), userKey(batch.getId()), CLAIM_QUEUE_KEY),
                String.valueOf(userId),
                payload,
                String.valueOf(ttlSeconds(batch))
        );
        if (result == null) {
            return new ClaimResult(false, "REDIS_ERROR");
        }
        if (result == 0) {
            return new ClaimResult(true, "OK");
        }
        if (result == 1) {
            return new ClaimResult(false, "OUT_OF_STOCK");
        }
        return new ClaimResult(false, "DUPLICATE");
    }

    private String stockKey(Long batchId) {
        return STOCK_KEY_PREFIX + batchId + STOCK_KEY_SUFFIX;
    }

    private String userKey(Long batchId) {
        return STOCK_KEY_PREFIX + batchId + USER_KEY_SUFFIX;
    }

    private long ttlSeconds(CouponBatch batch) {
        LocalDateTime expireTime = batch.getExpireTime();
        if (expireTime == null) {
            return TimeUnit.DAYS.toSeconds(COUPON_REDIS_RETENTION_DAYS);
        }
        LocalDateTime cleanupAt = expireTime.plusDays(COUPON_REDIS_RETENTION_DAYS);
        long seconds = Duration.between(LocalDateTime.now(), cleanupAt).getSeconds();
        return Math.max(seconds, MIN_TTL_SECONDS);
    }
}
