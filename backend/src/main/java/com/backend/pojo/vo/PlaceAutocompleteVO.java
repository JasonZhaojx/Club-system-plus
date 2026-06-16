package com.backend.pojo.vo;

import lombok.Data;

@Data
public class PlaceAutocompleteVO {
    private String placeId;
    private String mainText;
    private String secondaryText;
    private String fullText;
}
