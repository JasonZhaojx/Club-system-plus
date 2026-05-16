package com.backend.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserMembershipVO {
    private Long departmentId;
    private String departmentName;
    private LocalDateTime joinedAt;
}
