package com.backend.pojo.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RestaurantReviewVO {
    private Long id;
    private Long restaurantId;
    private Long userId;
    private String username;
    private String nickname;
    private String avatarUrl;
    private Integer rating;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
