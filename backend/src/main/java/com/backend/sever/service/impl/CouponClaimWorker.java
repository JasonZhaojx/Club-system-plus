package com.backend.sever.service.impl;

import com.backend.pojo.entity.CouponClaimTask;
import com.backend.pojo.entity.CouponClaimTaskStatus;
import com.backend.pojo.entity.UserCoupon;
import com.backend.pojo.entity.UserCouponStatus;
import com.backend.sever.mapper.CouponBatchMapper;
import com.backend.sever.mapper.CouponClaimTaskMapper;
import com.backend.sever.mapper.UserCouponMapper;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

@Component
public class CouponClaimWorker {
    private final CouponBatchMapper couponBatchMapper;
    private final UserCouponMapper userCouponMapper;
    private final CouponClaimTaskMapper couponClaimTaskMapper;
    private final TransactionTemplate transactionTemplate;

    public CouponClaimWorker(
            CouponBatchMapper couponBatchMapper,
            UserCouponMapper userCouponMapper,
            CouponClaimTaskMapper couponClaimTaskMapper,
            TransactionTemplate transactionTemplate
    ) {
        this.couponBatchMapper = couponBatchMapper;
        this.userCouponMapper = userCouponMapper;
        this.couponClaimTaskMapper = couponClaimTaskMapper;
        this.transactionTemplate = transactionTemplate;
    }

    @RabbitListener(queues = "${app.rabbitmq.coupon-claim-queue}")
    public void handleCouponClaimMessage(String taskIdPayload) {
        try {
            Long taskId = Long.valueOf(taskIdPayload);
            CouponClaimTask task = couponClaimTaskMapper.selectById(taskId);
            if (task != null) {
                processTask(task);
            }
        } catch (RuntimeException ignored) {
            // Malformed messages are ignored. Durable PENDING tasks are covered by compensation.
        }
    }

    @Scheduled(fixedDelay = 5000)
    public void compensateFailedTasks() {
        List<CouponClaimTask> tasks = couponClaimTaskMapper.selectRetryableTasks(50);
        for (CouponClaimTask task : tasks) {
            processTask(task);
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
        UserCoupon coupon = new UserCoupon();
        coupon.setBatchId(task.getBatchId());
        coupon.setUserId(task.getUserId());
        coupon.setStatus(UserCouponStatus.UNUSED);
        try {
            userCouponMapper.insert(coupon);
        } catch (DuplicateKeyException ignored) {
            couponClaimTaskMapper.updateStatus(task.getId(), CouponClaimTaskStatus.DONE, null);
            return;
        }
        if (couponBatchMapper.incrementClaimedCount(task.getBatchId()) == 0) {
            throw new IllegalStateException("MySQL stock is empty");
        }
        couponClaimTaskMapper.updateStatus(task.getId(), CouponClaimTaskStatus.DONE, null);
    }
}
