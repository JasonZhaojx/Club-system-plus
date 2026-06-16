package com.backend.pojo.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class RestaurantVO {
    private Long id;
    private String name;
    private String address;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String category;
    private String priceLevel;
    private String websiteUrl;
    private String coverUrl;
    private String status;
    private BigDecimal ratingAvg;
    private Integer reviewCount;
    private Integer distanceMeters;
    private Double recommendScore;
}
