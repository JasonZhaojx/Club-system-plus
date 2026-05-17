package com.backend.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
/**
 * used to get the created activity
 */
public class ActivityCreateDTO {
    private String title;
    private String summary;
    private String detail;
    private String category;
    private String categoryName;
    private String imageUrl;
    private String location;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer capacity;
    private String requiredRoleCode;
}
