package com.backend.sever.service.impl;

import com.backend.common.auth.UserPrincipal;
import com.backend.pojo.dto.CouponBatchCreateDTO;
import com.backend.pojo.dto.CouponUseDTO;
import com.backend.pojo.entity.CouponBatch;
import com.backend.pojo.entity.CouponBatchStatus;
import com.backend.pojo.entity.CouponClaimTask;
import com.backend.pojo.entity.CouponClaimTaskStatus;
import com.backend.pojo.entity.CouponRedemption;
import com.backend.pojo.entity.UserCoupon;
import com.backend.pojo.entity.UserCouponStatus;
import com.backend.pojo.vo.CouponBatchVO;
import com.backend.pojo.vo.CouponRedemptionVO;
import com.backend.pojo.vo.PageVO;
import com.backend.pojo.vo.UserCouponVO;
import com.backend.sever.exception.BusinessException;
import com.backend.sever.exception.ErrorCode;
import com.backend.sever.config.SentinelResourceNames;
import com.backend.sever.mapper.CouponBatchMapper;
import com.backend.sever.mapper.CouponClaimTaskMapper;
import com.backend.sever.mapper.CouponRedemptionMapper;
import com.backend.sever.mapper.UserCouponMapper;
import com.backend.sever.service.BusinessRateLimiter;
import com.backend.sever.service.CouponSeckillService;
import com.backend.sever.service.SentinelGuard;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CouponServiceImpl implements com.backend.sever.service.CouponService {
    private final CouponBatchMapper couponBatchMapper;
    private final UserCouponMapper userCouponMapper;
    private final CouponRedemptionMapper couponRedemptionMapper;
    private final CouponClaimTaskMapper couponClaimTaskMapper;
    private final CouponSeckillService couponSeckillService;
    private final CouponClaimMessageProducer couponClaimMessageProducer;
    private final BusinessRateLimiter businessRateLimiter;
    private final SentinelGuard sentinelGuard;

    public CouponServiceImpl(
            CouponBatchMapper couponBatchMapper,
            UserCouponMapper userCouponMapper,
            CouponRedemptionMapper couponRedemptionMapper,
            CouponClaimTaskMapper couponClaimTaskMapper,
            CouponSeckillService couponSeckillService,
            CouponClaimMessageProducer couponClaimMessageProducer,
            BusinessRateLimiter businessRateLimiter,
            SentinelGuard sentinelGuard
    ) {
        this.couponBatchMapper = couponBatchMapper;
        this.userCouponMapper = userCouponMapper;
        this.couponRedemptionMapper = couponRedemptionMapper;
        this.couponClaimTaskMapper = couponClaimTaskMapper;
        this.couponSeckillService = couponSeckillService;
        this.couponClaimMessageProducer = couponClaimMessageProducer;
        this.businessRateLimiter = businessRateLimiter;
        this.sentinelGuard = sentinelGuard;
    }

    @Override
    public PageVO<CouponBatchVO> listBatches(String keyword, CouponBatchStatus status, int page, int size) {
        return listBatchPage(keyword, status, page, size);
    }

    @Override
    public PageVO<CouponBatchVO> listClaimableBatches(UserPrincipal principal, String keyword, int page, int size) {
        PageVO<CouponBatchVO> pageResult = listBatchPage(keyword, CouponBatchStatus.ACTIVE, page, size);
        List<CouponBatchVO> matched = pageResult.getRecords()
                .stream()
                .filter(batch -> rolesAllowed(principal.roles(), batch.allowedRoleCodes()))
                .toList();
        return new PageVO<>(matched, matched.size(), pageResult.getPage(), pageResult.getSize());
    }

    @Override
    @Transactional
    public CouponBatchVO createBatch(UserPrincipal principal, CouponBatchCreateDTO request) {
        CouponBatch batch = new CouponBatch();
        fillBatch(batch, request);
        batch.setClaimedCount(0);
        batch.setStatus(CouponBatchStatus.ACTIVE);
        batch.setCreatorId(principal.userId());
        couponBatchMapper.insert(batch);
        CouponBatch created = couponBatchMapper.selectById(batch.getId());
        couponSeckillService.preloadCoupon(created);
        return CouponBatchVO.from(created);
    }

    @Override
    @Transactional
    public CouponBatchVO updateBatch(Long batchId, CouponBatchCreateDTO request) {
        CouponBatch batch = requireBatch(batchId);
        fillBatch(batch, request);
        if (batch.getStock() < batch.getClaimedCount()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Stock cannot be lower than claimed count");
        }
        couponBatchMapper.updateById(batch);
        CouponBatch updated = couponBatchMapper.selectById(batchId);
        couponSeckillService.preloadCoupon(updated);
        return CouponBatchVO.from(updated);
    }

    @Override
    @Transactional
    public UserCouponVO claimCoupon(UserPrincipal principal, Long batchId) {
        businessRateLimiter.checkCouponClaim(principal.userId(), batchId);
        try (SentinelGuard.GuardEntry guard = sentinelGuard.enter(SentinelResourceNames.COUPON_CLAIM, batchId)) {
            try {
                return doClaimCoupon(principal, batchId);
            } catch (RuntimeException exception) {
                guard.trace(exception);
                throw exception;
            }
        }
    }

    private UserCouponVO doClaimCoupon(UserPrincipal principal, Long batchId) {
        CouponBatch batch = requireBatch(batchId);
        LocalDateTime now = LocalDateTime.now();
        if (batch.getStatus() != CouponBatchStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.CONFLICT, "Coupon batch is not active");
        }
        if (now.isBefore(batch.getClaimStartTime()) || now.isAfter(batch.getClaimEndTime())) {
            throw new BusinessException(ErrorCode.CONFLICT, "Coupon is outside claim time");
        }
        if (now.isAfter(batch.getExpireTime())) {
            throw new BusinessException(ErrorCode.CONFLICT, "Coupon has expired");
        }
        if (!roleAllowed(principal.roles(), batch.getAllowedRoleCodes())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Current role cannot claim this coupon");
        }
        if (userCouponMapper.selectByBatchAndUser(batchId, principal.userId()) != null
                || couponClaimTaskMapper.selectByBatchAndUser(batchId, principal.userId()) != null) {
            throw new BusinessException(ErrorCode.CONFLICT, "Cannot claim the same coupon twice");
        }
        CouponSeckillService.ClaimResult claimResult = couponSeckillService.tryClaim(batch, principal.userId());
        if (!claimResult.success() && "DUPLICATE".equals(claimResult.code())) {
            throw new BusinessException(ErrorCode.CONFLICT, "Cannot claim the same coupon twice");
        }
        if (!claimResult.success() && "OUT_OF_STOCK".equals(claimResult.code())) {
            throw new BusinessException(ErrorCode.CONFLICT, "Coupon stock is empty");
        }
        if (!claimResult.success()) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Coupon claim queue is unavailable");
        }
        CouponClaimTask task = createClaimTaskIfAbsent(batchId, principal.userId());
        couponClaimMessageProducer.sendAfterCommit(task.getId());
        return new UserCouponVO(
                null,
                batch.getId(),
                batch.getName(),
                batch.getDescription(),
                batch.getCouponType(),
                batch.getBenefitText(),
                UserCouponStatus.UNUSED,
                now,
                null,
                batch.getExpireTime()
        );
    }

    @Override
    public List<UserCouponVO> listMyCoupons(UserPrincipal principal) {
        return userCouponMapper.selectUserCoupons(principal.userId());
    }

    @Override
    @Transactional
    public UserCouponVO useCoupon(UserPrincipal principal, Long userCouponId, CouponUseDTO request) {
        UserCouponVO coupon = userCouponMapper.selectUserCouponDetail(userCouponId, principal.userId());
        if (coupon == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Coupon does not exist");
        }
        if (coupon.status() != UserCouponStatus.UNUSED) {
            throw new BusinessException(ErrorCode.CONFLICT, "Coupon is not usable");
        }
        if (LocalDateTime.now().isAfter(coupon.expireTime())) {
            userCouponMapper.updateStatus(userCouponId, principal.userId(), UserCouponStatus.EXPIRED);
            throw new BusinessException(ErrorCode.CONFLICT, "Coupon has expired");
        }
        if (userCouponMapper.updateStatus(userCouponId, principal.userId(), UserCouponStatus.USED) == 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "Coupon is not usable");
        }
        CouponRedemption redemption = new CouponRedemption();
        redemption.setUserCouponId(userCouponId);
        redemption.setBatchId(coupon.batchId());
        redemption.setUserId(principal.userId());
        redemption.setScene(normalizeOptional(request == null ? null : request.getScene(), 80));
        redemption.setNote(normalizeOptional(request == null ? null : request.getNote(), 255));
        couponRedemptionMapper.insert(redemption);
        return userCouponMapper.selectUserCouponDetail(userCouponId, principal.userId());
    }

    @Override
    public List<CouponRedemptionVO> listMyRedemptions(UserPrincipal principal) {
        return couponRedemptionMapper.selectUserRedemptions(principal.userId());
    }

    private PageVO<CouponBatchVO> listBatchPage(String keyword, CouponBatchStatus status, int page, int size) {
        int normalizedPage = Math.max(page, 1);
        int normalizedSize = Math.min(Math.max(size, 1), 50);
        int offset = (normalizedPage - 1) * normalizedSize;
        String normalizedKeyword = StringUtils.hasText(keyword) ? keyword.trim() : null;
        return new PageVO<>(
                couponBatchMapper.selectBatchPage(normalizedKeyword, status, offset, normalizedSize)
                        .stream()
                        .map(CouponBatchVO::from)
                        .toList(),
                couponBatchMapper.countBatches(normalizedKeyword, status),
                normalizedPage,
                normalizedSize
        );
    }

    private CouponBatch requireBatch(Long batchId) {
        if (batchId == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Coupon batch id is required");
        }
        CouponBatch batch = couponBatchMapper.selectById(batchId);
        if (batch == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Coupon batch does not exist");
        }
        return batch;
    }

    private void fillBatch(CouponBatch batch, CouponBatchCreateDTO request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Coupon batch data is required");
        }
        if (request.getClaimStartTime() == null || request.getClaimEndTime() == null
                || !request.getClaimEndTime().isAfter(request.getClaimStartTime())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Claim time range is invalid");
        }
        if (request.getExpireTime() == null || !request.getExpireTime().isAfter(request.getClaimStartTime())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Expire time is invalid");
        }
        if (request.getStock() == null || request.getStock() < 1 || request.getStock() > 100000) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Stock must be between 1 and 100000");
        }
        batch.setName(requireText(request.getName(), "Coupon name is required", 120));
        batch.setDescription(normalizeOptional(request.getDescription(), 255));
        batch.setCouponType(requireText(request.getCouponType(), "Coupon type is required", 50));
        batch.setBenefitText(requireText(request.getBenefitText(), "Coupon benefit is required", 120));
        batch.setStock(request.getStock());
        batch.setClaimStartTime(request.getClaimStartTime());
        batch.setClaimEndTime(request.getClaimEndTime());
        batch.setExpireTime(request.getExpireTime());
        batch.setAllowedRoleCodes(joinRoles(request.getAllowedRoleCodes()));
    }

    private String joinRoles(List<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return null;
        }
        return String.join(",", roles.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .toList());
    }

    private boolean roleAllowed(List<String> userRoles, String allowedRoleCodes) {
        if (userRoles.contains("SYSTEM_MAINTAINER")) {
            return true;
        }
        if (!StringUtils.hasText(allowedRoleCodes)) {
            return true;
        }
        for (String role : allowedRoleCodes.split(",")) {
            if (roleSatisfies(userRoles, role.trim())) {
                return true;
            }
        }
        return false;
    }

    private boolean rolesAllowed(List<String> userRoles, List<String> allowedRoleCodes) {
        if (userRoles.contains("SYSTEM_MAINTAINER")) {
            return true;
        }
        if (allowedRoleCodes == null || allowedRoleCodes.isEmpty()) {
            return true;
        }
        return allowedRoleCodes.stream().anyMatch(role -> roleSatisfies(userRoles, role));
    }

    private CouponClaimTask createClaimTaskIfAbsent(Long batchId, Long userId) {
        CouponClaimTask task = new CouponClaimTask();
        task.setBatchId(batchId);
        task.setUserId(userId);
        task.setStatus(CouponClaimTaskStatus.PENDING);
        try {
            couponClaimTaskMapper.insert(task);
        } catch (DuplicateKeyException ignored) {
            // The Redis user set already prevents duplicate claims. The DB unique key is the durable fallback.
            task = couponClaimTaskMapper.selectByBatchAndUser(batchId, userId);
        }
        return task;
    }

    private boolean roleSatisfies(List<String> roles, String requiredRoleCode) {
        return switch (requiredRoleCode) {
            case "REGISTERED_USER" -> !roles.isEmpty();
            case "CLUB_MEMBER" -> roles.contains("CLUB_MEMBER")
                    || roles.contains("DEPARTMENT_LEADER")
                    || roles.contains("PRESIDENT");
            case "DEPARTMENT_LEADER" -> roles.contains("DEPARTMENT_LEADER") || roles.contains("PRESIDENT");
            case "PRESIDENT" -> roles.contains("PRESIDENT");
            default -> roles.contains(requiredRoleCode);
        };
    }

    private String requireText(String value, String message, int maxLength) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, message);
        }
        String trimmed = value.trim();
        if (trimmed.length() > maxLength) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Field length exceeds " + maxLength);
        }
        return trimmed;
    }

    private String normalizeOptional(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() > maxLength) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Field length exceeds " + maxLength);
        }
        return trimmed;
    }
}
