package com.backend.sever.service;

import com.backend.common.auth.UserPrincipal;
import com.backend.pojo.dto.CouponBatchCreateDTO;
import com.backend.pojo.dto.CouponUseDTO;
import com.backend.pojo.entity.CouponBatchStatus;
import com.backend.pojo.vo.CouponBatchVO;
import com.backend.pojo.vo.CouponRedemptionVO;
import com.backend.pojo.vo.PageVO;
import com.backend.pojo.vo.UserCouponVO;

import java.util.List;

public interface CouponService {
    PageVO<CouponBatchVO> listBatches(String keyword, CouponBatchStatus status, int page, int size);

    PageVO<CouponBatchVO> listClaimableBatches(UserPrincipal principal, String keyword, int page, int size);

    CouponBatchVO createBatch(UserPrincipal principal, CouponBatchCreateDTO request);

    CouponBatchVO updateBatch(Long batchId, CouponBatchCreateDTO request);

    UserCouponVO claimCoupon(UserPrincipal principal, Long batchId);

    List<UserCouponVO> listMyCoupons(UserPrincipal principal);

    UserCouponVO useCoupon(UserPrincipal principal, Long userCouponId, CouponUseDTO request);

    List<CouponRedemptionVO> listMyRedemptions(UserPrincipal principal);
}
