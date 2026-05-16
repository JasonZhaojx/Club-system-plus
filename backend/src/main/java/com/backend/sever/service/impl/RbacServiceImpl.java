package com.backend.sever.service.impl;

import com.backend.pojo.dto.AssignRolePermissionDTO;
import com.backend.pojo.dto.AssignUserRoleDTO;
import com.backend.pojo.vo.PermissionVO;
import com.backend.pojo.vo.RoleVO;
import com.backend.sever.exception.BusinessException;
import com.backend.sever.exception.ErrorCode;
import com.backend.sever.mapper.PermissionMapper;
import com.backend.sever.mapper.RoleMapper;
import com.backend.sever.mapper.UserMapper;
import com.backend.sever.service.RbacService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class RbacServiceImpl implements RbacService {
    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;
    private final UserMapper userMapper;

    public RbacServiceImpl(
            RoleMapper roleMapper,
            PermissionMapper permissionMapper,
            UserMapper userMapper
    ) {
        this.roleMapper = roleMapper;
        this.permissionMapper = permissionMapper;
        this.userMapper = userMapper;
    }

    @Override
    public List<RoleVO> listRoles() {
        return roleMapper.selectList(null).stream().map(RoleVO::from).toList();
    }

    @Override
    public List<PermissionVO> listPermissions() {
        return permissionMapper.selectList(null).stream().map(PermissionVO::from).toList();
    }

    @Override
    @Transactional
    public void assignUserRoles(AssignUserRoleDTO request) {
        if (request == null || request.getUserId() == null || CollectionUtils.isEmpty(request.getRoleCodes())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "用户 ID 和角色不能为空");
        }
        if (userMapper.selectById(request.getUserId()) == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        roleMapper.deleteUserRoles(request.getUserId());
        request.getRoleCodes().stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .forEach(roleCode -> roleMapper.insertUserRoleByCode(request.getUserId(), roleCode));
    }

    @Override
    @Transactional
    public void assignRolePermissions(AssignRolePermissionDTO request) {
        if (request == null
                || !StringUtils.hasText(request.getRoleCode())
                || CollectionUtils.isEmpty(request.getPermissionCodes())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "角色编码和权限编码不能为空");
        }
        String roleCode = request.getRoleCode().trim();
        permissionMapper.deleteRolePermissions(roleCode);
        request.getPermissionCodes().stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .forEach(permissionCode -> permissionMapper.insertRolePermissionByCode(roleCode, permissionCode));
    }
}
