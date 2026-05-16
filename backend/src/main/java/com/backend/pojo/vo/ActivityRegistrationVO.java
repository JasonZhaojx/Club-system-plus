package com.backend.pojo.vo;

import com.backend.pojo.entity.ActivityRegistrationStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActivityRegistrationVO {
    private Long id;
    private Long activityId;
    private String activityTitle;
    private String activityImageUrl;
    private String activityLocation;
    private LocalDateTime activityStartTime;
    private ActivityRegistrationStatus status;
    private LocalDateTime registeredAt;
}
