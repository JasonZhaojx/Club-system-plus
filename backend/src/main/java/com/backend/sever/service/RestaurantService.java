package com.backend.sever.service;

import com.backend.common.auth.UserPrincipal;
import com.backend.pojo.dto.RestaurantReviewDTO;
import com.backend.pojo.vo.PageVO;
import com.backend.pojo.vo.RestaurantReviewVO;
import com.backend.pojo.vo.RestaurantVO;

import java.math.BigDecimal;

public interface RestaurantService {
    PageVO<RestaurantVO> listNearby(BigDecimal latitude, BigDecimal longitude, Integer radiusMeters, String category, int page, int size);

    RestaurantVO getRestaurant(Long restaurantId);

    PageVO<RestaurantReviewVO> listReviews(Long restaurantId, int page, int size);

    RestaurantReviewVO saveReview(UserPrincipal principal, Long restaurantId, RestaurantReviewDTO request);
}
