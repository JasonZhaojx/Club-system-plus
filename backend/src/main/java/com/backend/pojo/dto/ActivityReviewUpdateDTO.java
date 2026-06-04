package com.backend.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActivityReviewUpdateDTO {
    private String reviewImageUrl;
    private String reviewContent;
}
