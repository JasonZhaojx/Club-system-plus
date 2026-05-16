package com.backend.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssignRolePermissionDTO {
    private String roleCode;
    private List<String> permissionCodes;
}
