package com.backend.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("coupon_batch")

/**
 * create the basic of the coupon batch
 */
public class CouponBatch {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String description;
    private String couponType;
    private String benefitText;
    private Integer stock;
    private Integer claimedCount;
    private LocalDateTime claimStartTime;
    private LocalDateTime claimEndTime;
    private LocalDateTime expireTime;
    private String allowedRoleCodes;
    private CouponBatchStatus status;
    private Long creatorId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
