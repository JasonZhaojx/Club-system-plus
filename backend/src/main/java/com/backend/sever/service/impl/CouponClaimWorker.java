package com.backend.sever.service.impl;

import com.backend.pojo.entity.CouponClaimTask;
import com.backend.pojo.entity.CouponClaimTaskStatus;
import com.backend.pojo.entity.UserCoupon;
import com.backend.pojo.entity.UserCouponStatus;
import com.backend.sever.mapper.CouponBatchMapper;
import com.backend.sever.mapper.CouponClaimTaskMapper;
import com.backend.sever.mapper.UserCouponMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

@Component
public class CouponClaimWorker {
    private final StringRedisTemplate redisTemplate;
    private final CouponBatchMapper couponBatchMapper;
    private final UserCouponMapper userCouponMapper;
    private final CouponClaimTaskMapper couponClaimTaskMapper;
    private final TransactionTemplate transactionTemplate;

    public CouponClaimWorker(
            StringRedisTemplate redisTemplate,
            CouponBatchMapper couponBatchMapper,
            UserCouponMapper userCouponMapper,
            CouponClaimTaskMapper couponClaimTaskMapper,
            TransactionTemplate transactionTemplate
    ) {
        this.redisTemplate = redisTemplate;
        this.couponBatchMapper = couponBatchMapper;
        this.userCouponMapper = userCouponMapper;
        this.couponClaimTaskMapper = couponClaimTaskMapper;
        this.transactionTemplate = transactionTemplate;
    }

    @Scheduled(fixedDelay = 300)
    public void drainClaimQueue() {
        for (int i = 0; i < 100; i++) {
            String payload = redisTemplate.opsForList().leftPop(RedisCouponSeckillService.CLAIM_QUEUE_KEY);
            if (payload == null) {
                return;
            }
            handlePayload(payload);
        }
    }

    @Scheduled(fixedDelay = 5000)
    public void compensateFailedTasks() {
        List<CouponClaimTask> tasks = couponClaimTaskMapper.selectRetryableTasks(50);
        for (CouponClaimTask task : tasks) {
            processTask(task);
        }
    }

    private void handlePayload(String payload) {
        String[] parts = payload.split(":");
        if (parts.length != 2) {
            return;
        }
        try {
            Long batchId = Long.valueOf(parts[0]);
            Long userId = Long.valueOf(parts[1]);
            CouponClaimTask task = couponClaimTaskMapper.selectByBatchAndUser(batchId, userId);
            if (task == null) {
                task = new CouponClaimTask();
                task.setBatchId(batchId);
                task.setUserId(userId);
                task.setStatus(CouponClaimTaskStatus.PENDING);
                try {
                    couponClaimTaskMapper.insert(task);
                } catch (DuplicateKeyException ignored) {
                    task = couponClaimTaskMapper.selectByBatchAndUser(batchId, userId);
                }
            }
            if (task != null) {
                processTask(task);
            }
        } catch (RuntimeException ignored) {
            // Malformed or transient failures are covered by the durable task compensation path.
        }
    }

    public void processTask(CouponClaimTask task) {
        try {
            transactionTemplate.executeWithoutResult(status -> processTaskInTransaction(task));
        } catch (RuntimeException ex) {
            String message = ex.getMessage();
            couponClaimTaskMapper.updateStatus(
                    task.getId(),
                    CouponClaimTaskStatus.FAILED,
                    message == null ? ex.getClass().getSimpleName() : message.substring(0, Math.min(message.length(), 500))
            );
        }
    }

    private void processTaskInTransaction(CouponClaimTask task) {
        if (userCouponMapper.selectByBatchAndUser(task.getBatchId(), task.getUserId()) != null) {
            couponClaimTaskMapper.updateStatus(task.getId(), CouponClaimTaskStatus.DONE, null);
            return;
        }
        if (couponBatchMapper.incrementClaimedCount(task.getBatchId()) == 0) {
            couponClaimTaskMapper.updateStatus(task.getId(), CouponClaimTaskStatus.FAILED, "MySQL stock is empty");
            return;
        }
        UserCoupon coupon = new UserCoupon();
        coupon.setBatchId(task.getBatchId());
        coupon.setUserId(task.getUserId());
        coupon.setStatus(UserCouponStatus.UNUSED);
        userCouponMapper.insert(coupon);
        couponClaimTaskMapper.updateStatus(task.getId(), CouponClaimTaskStatus.DONE, null);
    }
}
