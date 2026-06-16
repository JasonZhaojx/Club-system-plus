package com.backend.sever.controller;

import com.backend.common.auth.UserPrincipal;
import com.backend.pojo.dto.RestaurantReviewDTO;
import com.backend.pojo.vo.PageVO;
import com.backend.pojo.vo.RestaurantReviewVO;
import com.backend.pojo.vo.RestaurantVO;
import com.backend.sever.common.Result;
import com.backend.sever.exception.BusinessException;
import com.backend.sever.exception.ErrorCode;
import com.backend.sever.service.RestaurantService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/restaurants")
public class RestaurantController {
    private final RestaurantService restaurantService;

    public RestaurantController(RestaurantService restaurantService) {
        this.restaurantService = restaurantService;
    }

    @GetMapping("/nearby")
    public Result<PageVO<RestaurantVO>> listNearby(
            @RequestParam BigDecimal lat,
            @RequestParam BigDecimal lng,
            @RequestParam(required = false) Integer radius,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return Result.success(restaurantService.listNearby(lat, lng, radius, category, page, size));
    }

    @GetMapping("/{restaurantId}")
    public Result<RestaurantVO> getRestaurant(@PathVariable Long restaurantId) {
        return Result.success(restaurantService.getRestaurant(restaurantId));
    }

    @GetMapping("/{restaurantId}/reviews")
    public Result<PageVO<RestaurantReviewVO>> listReviews(
            @PathVariable Long restaurantId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return Result.success(restaurantService.listReviews(restaurantId, page, size));
    }

    @PostMapping("/{restaurantId}/reviews")
    public Result<RestaurantReviewVO> saveReview(
            Authentication authentication,
            @PathVariable Long restaurantId,
            @RequestBody RestaurantReviewDTO request
    ) {
        return Result.success(restaurantService.saveReview(currentPrincipal(authentication), restaurantId, request));
    }

    private UserPrincipal currentPrincipal(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return principal;
    }
}
