package com.backend.sever.service;

import com.backend.pojo.entity.CouponBatch;

public interface CouponSeckillService {
    void preloadCoupon(CouponBatch batch);

    ClaimResult tryClaim(CouponBatch batch, Long userId);

    record ClaimResult(boolean success, String code) {
    }
}
