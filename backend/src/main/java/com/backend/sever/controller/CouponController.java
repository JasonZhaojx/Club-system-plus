package com.backend.sever.controller;

import com.backend.common.auth.UserPrincipal;
import com.backend.pojo.dto.CouponBatchCreateDTO;
import com.backend.pojo.dto.CouponUseDTO;
import com.backend.pojo.entity.CouponBatchStatus;
import com.backend.pojo.vo.CouponBatchVO;
import com.backend.pojo.vo.CouponRedemptionVO;
import com.backend.pojo.vo.PageVO;
import com.backend.pojo.vo.UserCouponVO;
import com.backend.sever.common.Result;
import com.backend.sever.exception.BusinessException;
import com.backend.sever.exception.ErrorCode;
import com.backend.sever.service.CouponService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/coupons")
public class CouponController {
    private final CouponService couponService;

    public CouponController(CouponService couponService) {
        this.couponService = couponService;
    }

    @GetMapping("/batches")
    public Result<PageVO<CouponBatchVO>> listClaimableBatches(
            Authentication authentication,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return Result.success(couponService.listClaimableBatches(currentPrincipal(authentication), keyword, page, size));
    }

    @GetMapping("/batches/manage")
    @PreAuthorize("hasAnyAuthority('coupon:manage', 'system:maintain')")
    public Result<PageVO<CouponBatchVO>> listManageBatches(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) CouponBatchStatus status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return Result.success(couponService.listBatches(keyword, status, page, size));
    }

    @PostMapping("/batches")
    @PreAuthorize("hasAnyAuthority('coupon:manage', 'system:maintain')")
    public Result<CouponBatchVO> createBatch(Authentication authentication, @RequestBody CouponBatchCreateDTO request) {
        return Result.success(couponService.createBatch(currentPrincipal(authentication), request));
    }

    @PutMapping("/batches/{batchId}")
    @PreAuthorize("hasAnyAuthority('coupon:manage', 'system:maintain')")
    public Result<CouponBatchVO> updateBatch(@PathVariable Long batchId, @RequestBody CouponBatchCreateDTO request) {
        return Result.success(couponService.updateBatch(batchId, request));
    }

    @PostMapping("/batches/{batchId}/claim")
    @PreAuthorize("hasAnyAuthority('coupon:grab', 'system:maintain')")
    public Result<UserCouponVO> claimCoupon(Authentication authentication, @PathVariable Long batchId) {
        return Result.success(couponService.claimCoupon(currentPrincipal(authentication), batchId));
    }

    @GetMapping("/me")
    public Result<List<UserCouponVO>> listMyCoupons(Authentication authentication) {
        return Result.success(couponService.listMyCoupons(currentPrincipal(authentication)));
    }

    @PatchMapping("/me/{userCouponId}/use")
    public Result<UserCouponVO> useCoupon(
            Authentication authentication,
            @PathVariable Long userCouponId,
            @RequestBody(required = false) CouponUseDTO request
    ) {
        return Result.success(couponService.useCoupon(currentPrincipal(authentication), userCouponId, request));
    }

    @GetMapping("/redemptions/me")
    public Result<List<CouponRedemptionVO>> listMyRedemptions(Authentication authentication) {
        return Result.success(couponService.listMyRedemptions(currentPrincipal(authentication)));
    }

    private UserPrincipal currentPrincipal(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return principal;
    }
}
