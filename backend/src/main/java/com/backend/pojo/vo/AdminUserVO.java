package com.backend.pojo.vo;

import com.backend.pojo.entity.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserVO {
    private Long id;
    private String username;
    private String nickname;
    private String email;
    private UserStatus status;
    private Long departmentId;
    private String departmentName;
    private LocalDateTime joinedAt;
    private String memberStatus;
    private Boolean departmentLeader;
    private String roles;
    private LocalDateTime createdAt;
}
