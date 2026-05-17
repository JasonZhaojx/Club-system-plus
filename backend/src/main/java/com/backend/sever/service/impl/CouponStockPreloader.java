package com.backend.sever.service.impl;

import com.backend.pojo.entity.CouponBatchStatus;
import com.backend.sever.mapper.CouponBatchMapper;
import com.backend.sever.service.CouponSeckillService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class CouponStockPreloader implements ApplicationRunner {
    private final CouponBatchMapper couponBatchMapper;
    private final CouponSeckillService couponSeckillService;

    public CouponStockPreloader(CouponBatchMapper couponBatchMapper, CouponSeckillService couponSeckillService) {
        this.couponBatchMapper = couponBatchMapper;
        this.couponSeckillService = couponSeckillService;
    }

    @Override
    public void run(ApplicationArguments args) {
        couponBatchMapper.selectBatchPage(null, CouponBatchStatus.ACTIVE, 0, 1000)
                .forEach(couponSeckillService::preloadCoupon);
    }
}
