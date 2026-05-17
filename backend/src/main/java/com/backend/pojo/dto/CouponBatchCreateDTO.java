package com.backend.pojo.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
/**
 * the coupon batch create dto
 */
public class CouponBatchCreateDTO {
    private String name;
    private String description;
    private String couponType;
    private String benefitText;
    private Integer stock;
    private LocalDateTime claimStartTime;
    private LocalDateTime claimEndTime;
    private LocalDateTime expireTime;
    private List<String> allowedRoleCodes;
}
