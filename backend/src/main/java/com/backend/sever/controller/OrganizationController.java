package com.backend.sever.controller;

import com.backend.common.auth.UserPrincipal;
import com.backend.pojo.dto.AssignDepartmentLeaderDTO;
import com.backend.pojo.dto.AssignMemberDepartmentDTO;
import com.backend.pojo.dto.CreateDepartmentDTO;
import com.backend.pojo.dto.UpdateDepartmentDTO;
import com.backend.pojo.dto.UpdateMemberStatusDTO;
import com.backend.pojo.vo.ClubMemberVO;
import com.backend.pojo.vo.DepartmentLeaderVO;
import com.backend.pojo.vo.DepartmentVO;
import com.backend.pojo.vo.AdminUserVO;
import com.backend.pojo.vo.PageVO;
import com.backend.sever.common.Result;
import com.backend.sever.exception.BusinessException;
import com.backend.sever.exception.ErrorCode;
import com.backend.sever.service.OrganizationService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/organization")
public class OrganizationController {
    private final OrganizationService organizationService;

    public OrganizationController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    @GetMapping("/departments")
    public Result<List<DepartmentVO>> listDepartments() {
        return Result.success(organizationService.listDepartments());
    }

    @PostMapping("/departments")
    @PreAuthorize("hasAnyAuthority('department:manage', 'system:maintain')")
    public Result<DepartmentVO> createDepartment(@RequestBody CreateDepartmentDTO request) {
        return Result.success(organizationService.createDepartment(request));
    }

    @PutMapping("/departments/{departmentId}")
    @PreAuthorize("hasAnyAuthority('department:manage', 'system:maintain')")
    public Result<DepartmentVO> updateDepartment(
            @PathVariable Long departmentId,
            @RequestBody UpdateDepartmentDTO request
    ) {
        return Result.success(organizationService.updateDepartment(departmentId, request));
    }

    @PatchMapping("/departments/{departmentId}/disable")
    @PreAuthorize("hasAnyAuthority('department:manage', 'system:maintain')")
    public Result<Void> disableDepartment(@PathVariable Long departmentId) {
        organizationService.disableDepartment(departmentId);
        return Result.success();
    }

    @PatchMapping("/departments/{departmentId}/enable")
    @PreAuthorize("hasAnyAuthority('department:manage', 'system:maintain')")
    public Result<Void> enableDepartment(@PathVariable Long departmentId) {
        organizationService.enableDepartment(departmentId);
        return Result.success();
    }

    @GetMapping("/members")
    @PreAuthorize("hasAnyAuthority('member:manage', 'department:manage', 'system:maintain')")
    public Result<List<ClubMemberVO>> listMembers(
            Authentication authentication,
            @RequestParam(required = false) Long departmentId
    ) {
        return Result.success(organizationService.listMembers(currentPrincipal(authentication), departmentId));
    }

    @GetMapping("/users")
    @PreAuthorize("hasAnyAuthority('department:manage', 'system:maintain')")
    public Result<PageVO<AdminUserVO>> listUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return Result.success(organizationService.listUsers(keyword, departmentId, page, size));
    }

    @PostMapping("/members")
    @PreAuthorize("hasAnyAuthority('member:manage', 'department:manage', 'system:maintain')")
    public Result<ClubMemberVO> assignMemberToDepartment(
            Authentication authentication,
            @RequestBody AssignMemberDepartmentDTO request
    ) {
        return Result.success(
                organizationService.assignMemberToDepartment(currentPrincipal(authentication), request)
        );
    }

    @PatchMapping("/members/status")
    @PreAuthorize("hasAnyAuthority('member:manage', 'department:manage', 'system:maintain')")
    public Result<ClubMemberVO> updateMemberStatus(
            Authentication authentication,
            @RequestBody UpdateMemberStatusDTO request
    ) {
        return Result.success(organizationService.updateMemberStatus(currentPrincipal(authentication), request));
    }

    @GetMapping("/leaders")
    @PreAuthorize("hasAnyAuthority('member:manage', 'department:manage', 'system:maintain')")
    public Result<List<DepartmentLeaderVO>> listLeaders(
            Authentication authentication,
            @RequestParam(required = false) Long departmentId
    ) {
        return Result.success(organizationService.listLeaders(currentPrincipal(authentication), departmentId));
    }

    @PostMapping("/leaders")
    @PreAuthorize("hasAnyAuthority('department:manage', 'system:maintain')")
    public Result<Void> appointDepartmentLeader(@RequestBody AssignDepartmentLeaderDTO request) {
        organizationService.appointDepartmentLeader(request);
        return Result.success();
    }

    @DeleteMapping("/leaders")
    @PreAuthorize("hasAnyAuthority('department:manage', 'system:maintain')")
    public Result<Void> removeDepartmentLeader(@RequestBody AssignDepartmentLeaderDTO request) {
        organizationService.removeDepartmentLeader(request);
        return Result.success();
    }

    private UserPrincipal currentPrincipal(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return principal;
    }
}
