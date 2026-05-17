package com.backend.pojo.vo;

import com.backend.pojo.entity.UserCouponStatus;

import java.time.LocalDateTime;

public record UserCouponVO(
        Long id,
        Long batchId,
        String batchName,
        String description,
        String couponType,
        String benefitText,
        UserCouponStatus status,
        LocalDateTime claimedAt,
        LocalDateTime usedAt,
        LocalDateTime expireTime
) {
}
