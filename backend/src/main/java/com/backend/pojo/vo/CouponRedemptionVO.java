package com.backend.pojo.vo;

import java.time.LocalDateTime;

public record CouponRedemptionVO(
        Long id,
        Long userCouponId,
        Long batchId,
        String batchName,
        String scene,
        String note,
        LocalDateTime redeemedAt
) {
}
