package com.backend.sever.service;

public interface BusinessRateLimiter {
    void checkCouponClaim(Long userId, Long batchId);

    void checkActivityRegister(Long userId, Long activityId);

    void checkPasswordResetEmail(String email, String ipAddress);
}
