package com.backend.sever.service.impl;

import com.backend.common.auth.UserPrincipal;
import com.backend.pojo.dto.RestaurantReviewDTO;
import com.backend.pojo.vo.PageVO;
import com.backend.pojo.vo.RestaurantReviewVO;
import com.backend.pojo.vo.RestaurantVO;
import com.backend.sever.exception.BusinessException;
import com.backend.sever.exception.ErrorCode;
import com.backend.sever.mapper.RestaurantMapper;
import com.backend.sever.mapper.RestaurantReviewMapper;
import com.backend.sever.service.RestaurantService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;

@Service
public class RestaurantServiceImpl implements RestaurantService {
    private static final double EARTH_RADIUS_METERS = 6_371_000D;
    private static final int DEFAULT_RADIUS_METERS = 1500;
    private static final int MAX_RADIUS_METERS = 5000;

    private final RestaurantMapper restaurantMapper;
    private final RestaurantReviewMapper restaurantReviewMapper;

    public RestaurantServiceImpl(RestaurantMapper restaurantMapper, RestaurantReviewMapper restaurantReviewMapper) {
        this.restaurantMapper = restaurantMapper;
        this.restaurantReviewMapper = restaurantReviewMapper;
    }

    @Override
    public PageVO<RestaurantVO> listNearby(BigDecimal latitude, BigDecimal longitude, Integer radiusMeters, String category, int page, int size) {
        double lat = requireCoordinate(latitude, -90, 90, "纬度不合法");
        double lng = requireCoordinate(longitude, -180, 180, "经度不合法");
        int radius = normalizeRadius(radiusMeters);
        int normalizedPage = Math.max(page, 1);
        int normalizedSize = Math.min(Math.max(size, 1), 50);

        double latDelta = Math.toDegrees(radius / EARTH_RADIUS_METERS);
        double lngDelta = Math.toDegrees(radius / (EARTH_RADIUS_METERS * Math.cos(Math.toRadians(lat))));
        BigDecimal minLat = BigDecimal.valueOf(lat - latDelta);
        BigDecimal maxLat = BigDecimal.valueOf(lat + latDelta);
        BigDecimal minLng = BigDecimal.valueOf(lng - lngDelta);
        BigDecimal maxLng = BigDecimal.valueOf(lng + lngDelta);
        String normalizedCategory = StringUtils.hasText(category) ? category.trim() : null;

        List<RestaurantVO> filtered = restaurantMapper
                .selectNearbyCandidates(minLat, maxLat, minLng, maxLng, normalizedCategory)
                .stream()
                .peek(item -> enrichDistanceAndScore(item, lat, lng, radius))
                .filter(item -> item.getDistanceMeters() != null && item.getDistanceMeters() <= radius)
                .sorted(Comparator
                        .comparing(RestaurantVO::getRecommendScore, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(RestaurantVO::getDistanceMeters))
                .toList();

        int fromIndex = Math.min((normalizedPage - 1) * normalizedSize, filtered.size());
        int toIndex = Math.min(fromIndex + normalizedSize, filtered.size());
        return new PageVO<>(filtered.subList(fromIndex, toIndex), filtered.size(), normalizedPage, normalizedSize);
    }

    @Override
    public RestaurantVO getRestaurant(Long restaurantId) {
        if (restaurantId == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "餐厅 ID 不能为空");
        }
        RestaurantVO restaurant = restaurantMapper.selectDetail(restaurantId);
        if (restaurant == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "餐厅不存在");
        }
        return restaurant;
    }

    @Override
    public PageVO<RestaurantReviewVO> listReviews(Long restaurantId, int page, int size) {
        getRestaurant(restaurantId);
        int normalizedPage = Math.max(page, 1);
        int normalizedSize = Math.min(Math.max(size, 1), 50);
        int offset = (normalizedPage - 1) * normalizedSize;
        return new PageVO<>(
                restaurantReviewMapper.selectReviewPage(restaurantId, offset, normalizedSize),
                restaurantReviewMapper.countReviews(restaurantId),
                normalizedPage,
                normalizedSize
        );
    }

    @Override
    @Transactional
    public RestaurantReviewVO saveReview(UserPrincipal principal, Long restaurantId, RestaurantReviewDTO request) {
        getRestaurant(restaurantId);
        if (request == null || request.getRating() == null || request.getRating() < 1 || request.getRating() > 5) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "评分必须在 1 到 5 之间");
        }
        String content = normalizeContent(request.getContent());
        restaurantReviewMapper.upsertReview(restaurantId, principal.userId(), request.getRating(), content);
        restaurantReviewMapper.refreshRatingStat(restaurantId);
        return restaurantReviewMapper.selectReviewPage(restaurantId, 0, 100)
                .stream()
                .filter(item -> item.getUserId().equals(principal.userId()))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_ERROR, "评论保存失败"));
    }

    private void enrichDistanceAndScore(RestaurantVO item, double lat, double lng, int radius) {
        double itemLat = item.getLatitude().doubleValue();
        double itemLng = item.getLongitude().doubleValue();
        int distance = (int) Math.round(haversineMeters(lat, lng, itemLat, itemLng));
        item.setDistanceMeters(distance);

        double ratingAvg = item.getRatingAvg() == null ? 0D : item.getRatingAvg().doubleValue();
        int reviewCount = item.getReviewCount() == null ? 0 : item.getReviewCount();
        double normalizedRating = ratingAvg / 5D;
        double reviewConfidence = Math.min(Math.log(reviewCount + 1D) / Math.log(51D), 1D);
        double distanceScore = Math.max(0D, 1D - ((double) distance / radius));
        double recommendScore = normalizedRating * 0.45D + reviewConfidence * 0.20D + distanceScore * 0.25D;
        item.setRecommendScore(BigDecimal.valueOf(recommendScore).setScale(4, RoundingMode.HALF_UP).doubleValue());
    }

    private double haversineMeters(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.pow(Math.sin(dLat / 2D), 2D)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.pow(Math.sin(dLng / 2D), 2D);
        return 2D * EARTH_RADIUS_METERS * Math.atan2(Math.sqrt(a), Math.sqrt(1D - a));
    }

    private double requireCoordinate(BigDecimal value, double min, double max, String message) {
        if (value == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, message);
        }
        double result = value.doubleValue();
        if (result < min || result > max) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, message);
        }
        return result;
    }

    private int normalizeRadius(Integer radiusMeters) {
        int radius = radiusMeters == null ? DEFAULT_RADIUS_METERS : radiusMeters;
        if (radius < 100 || radius > MAX_RADIUS_METERS) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "搜索半径必须在 100 到 5000 米之间");
        }
        return radius;
    }

    private String normalizeContent(String content) {
        if (!StringUtils.hasText(content)) {
            return null;
        }
        String trimmed = content.trim();
        if (trimmed.length() > 1000) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "评论不能超过 1000 个字符");
        }
        return trimmed;
    }
}
