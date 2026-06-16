package com.backend.pojo.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PlaceDetailVO {
    private String placeId;
    private String displayName;
    private String formattedAddress;
    private BigDecimal latitude;
    private BigDecimal longitude;
}
