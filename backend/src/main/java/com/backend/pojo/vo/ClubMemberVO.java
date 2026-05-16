package com.backend.pojo.vo;

import com.backend.pojo.entity.MemberStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClubMemberVO {
    private Long userId;
    private String username;
    private String nickname;
    private Long departmentId;
    private String departmentName;
    private LocalDateTime joinedAt;
    private MemberStatus status;
    private Boolean departmentLeader;
}
