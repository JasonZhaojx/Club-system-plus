package com.backend.pojo.dto;

import com.backend.pojo.entity.MemberStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateMemberStatusDTO {
    private Long userId;
    private MemberStatus status;
}
