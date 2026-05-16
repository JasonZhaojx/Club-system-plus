package com.backend.pojo.vo;

import com.backend.pojo.entity.Activity;
import com.backend.pojo.entity.ActivityStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActivityVO {
    private Long id;
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
    private Integer registeredCount;
    private ActivityStatus status;
    private String requiredRoleCode;
    private Long creatorId;
    private LocalDateTime publishedAt;

    public static ActivityVO from(Activity activity) {
        return new ActivityVO(
                activity.getId(),
                activity.getTitle(),
                activity.getSummary(),
                activity.getDetail(),
                activity.getCategory(),
                activity.getCategoryName(),
                activity.getImageUrl(),
                activity.getLocation(),
                activity.getStartTime(),
                activity.getEndTime(),
                activity.getCapacity(),
                activity.getRegisteredCount(),
                activity.getStatus(),
                activity.getRequiredRoleCode(),
                activity.getCreatorId(),
                activity.getPublishedAt()
        );
    }
}
