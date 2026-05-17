package com.backend.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("coupon_redemption")
public class CouponRedemption {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userCouponId;
    private Long batchId;
    private Long userId;
    private String scene;
    private String note;
    private LocalDateTime redeemedAt;
    private LocalDateTime createdAt;
}
