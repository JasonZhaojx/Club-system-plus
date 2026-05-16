package com.backend.sever.controller;

import com.backend.pojo.dto.AssignRolePermissionDTO;
import com.backend.pojo.dto.AssignUserRoleDTO;
import com.backend.pojo.vo.PermissionVO;
import com.backend.pojo.vo.RoleVO;
import com.backend.sever.common.Result;
import com.backend.sever.service.RbacService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/rbac")
public class RbacController {
    private final RbacService rbacService;

    public RbacController(RbacService rbacService) {
        this.rbacService = rbacService;
    }

    @GetMapping("/roles")
    @PreAuthorize("hasAnyAuthority('department:manage', 'system:maintain')")
    public Result<List<RoleVO>> listRoles() {
        return Result.success(rbacService.listRoles());
    }

    @GetMapping("/permissions")
    @PreAuthorize("hasAuthority('system:maintain')")
    public Result<List<PermissionVO>> listPermissions() {
        return Result.success(rbacService.listPermissions());
    }

    @PostMapping("/users/roles")
    @PreAuthorize("hasAnyAuthority('department:manage', 'system:maintain')")
    public Result<Void> assignUserRoles(@RequestBody AssignUserRoleDTO request) {
        rbacService.assignUserRoles(request);
        return Result.success();
    }

    @PostMapping("/roles/permissions")
    @PreAuthorize("hasAuthority('system:maintain')")
    public Result<Void> assignRolePermissions(@RequestBody AssignRolePermissionDTO request) {
        rbacService.assignRolePermissions(request);
        return Result.success();
    }
}
