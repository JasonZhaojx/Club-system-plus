package com.backend.sever.service;

import com.backend.pojo.dto.AssignRolePermissionDTO;
import com.backend.pojo.dto.AssignUserRoleDTO;
import com.backend.pojo.vo.PermissionVO;
import com.backend.pojo.vo.RoleVO;

import java.util.List;

public interface RbacService {
    List<RoleVO> listRoles();

    List<PermissionVO> listPermissions();

    void assignUserRoles(AssignUserRoleDTO request);

    void assignRolePermissions(AssignRolePermissionDTO request);
}
