package com.backend.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssignUserRoleDTO {
    private Long userId;
    private List<String> roleCodes;
}
