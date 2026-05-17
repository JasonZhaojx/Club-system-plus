package com.backend.pojo.vo;

import com.backend.pojo.entity.CouponBatch;
import com.backend.pojo.entity.CouponBatchStatus;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

public record CouponBatchVO(
        Long id,
        String name,
        String description,
        String couponType,
        String benefitText,
        Integer stock,
        Integer claimedCount,
        Integer remainingCount,
        LocalDateTime claimStartTime,
        LocalDateTime claimEndTime,
        LocalDateTime expireTime,
        List<String> allowedRoleCodes,
        CouponBatchStatus status
) {
    public static CouponBatchVO from(CouponBatch batch) {
        return new CouponBatchVO(
                batch.getId(),
                batch.getName(),
                batch.getDescription(),
                batch.getCouponType(),
                batch.getBenefitText(),
                batch.getStock(),
                batch.getClaimedCount(),
                Math.max(batch.getStock() - batch.getClaimedCount(), 0),
                batch.getClaimStartTime(),
                batch.getClaimEndTime(),
                batch.getExpireTime(),
                splitRoles(batch.getAllowedRoleCodes()),
                batch.getStatus()
        );
    }

    private static List<String> splitRoles(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .toList();
    }
}
