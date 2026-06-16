package com.backend.sever.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.backend.pojo.entity.RestaurantReview;
import com.backend.pojo.vo.RestaurantReviewVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface RestaurantReviewMapper extends BaseMapper<RestaurantReview> {
    List<RestaurantReviewVO> selectReviewPage(
            @Param("restaurantId") Long restaurantId,
            @Param("offset") int offset,
            @Param("size") int size
    );

    long countReviews(@Param("restaurantId") Long restaurantId);

    int upsertReview(
            @Param("restaurantId") Long restaurantId,
            @Param("userId") Long userId,
            @Param("rating") Integer rating,
            @Param("content") String content
    );

    int refreshRatingStat(@Param("restaurantId") Long restaurantId);
}
