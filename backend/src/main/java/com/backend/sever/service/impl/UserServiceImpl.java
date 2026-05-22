package com.backend.sever.service.impl;

import com.backend.pojo.dto.ChangePasswordDTO;
import com.backend.pojo.dto.UpdateProfileDTO;
import com.backend.pojo.entity.User;
import com.backend.pojo.entity.UserStatus;
import com.backend.pojo.vo.UserProfileVO;
import com.backend.sever.exception.BusinessException;
import com.backend.sever.exception.ErrorCode;
import com.backend.sever.mapper.PermissionMapper;
import com.backend.sever.mapper.RoleMapper;
import com.backend.sever.mapper.UserMapper;
import com.backend.sever.service.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class UserServiceImpl implements UserService {
    private static final String DEFAULT_AVATAR_URL = "https://ts1.tc.mm.bing.net/th/id/OIP-C.4n3KcdpOWTC32-U0LjDagwHaHa?cb=thfc1falcon&rs=1&pid=ImgDetMain&o=7&rm=3";

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(
            UserMapper userMapper,
            RoleMapper roleMapper,
            PermissionMapper permissionMapper,
            PasswordEncoder passwordEncoder
    ) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.permissionMapper = permissionMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserProfileVO currentUser(Long userId) {
        User user = findActiveUser(userId);
        return buildProfile(user);
    }

    @Override
    @Transactional
    public UserProfileVO updateProfile(Long userId, UpdateProfileDTO request) {
        if (request == null || !StringUtils.hasText(request.getNickname())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "昵称不能为空");
        }
        String nickname = request.getNickname().trim();
        if (nickname.length() > 50) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "昵称长度不能超过 50 个字符");
        }
        String email = StringUtils.hasText(request.getEmail()) ? request.getEmail().trim() : null;
        if (email != null && email.length() > 120) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "邮箱长度不能超过 120 个字符");
        }
        if (email != null && !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "邮箱格式不正确");
        }
        String avatarUrl = StringUtils.hasText(request.getAvatarUrl())
                ? request.getAvatarUrl().trim()
                : DEFAULT_AVATAR_URL;
        if (avatarUrl.length() > 500) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "头像地址长度不能超过 500 个字符");
        }
        if (!avatarUrl.equals(DEFAULT_AVATAR_URL) && !avatarUrl.startsWith("/api/files/images/avatar/")) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "头像必须通过上传生成");
        }
        findActiveUser(userId);
        userMapper.updateProfile(userId, nickname, email, avatarUrl);
        return currentUser(userId);
    }

    @Override
    @Transactional
    public void changePassword(Long userId, ChangePasswordDTO request) {
        if (request == null
                || !StringUtils.hasText(request.getOldPassword())
                || !StringUtils.hasText(request.getNewPassword())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "旧密码和新密码不能为空");
        }
        if (request.getNewPassword().length() < 8) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "新密码长度不能少于 8 个字符");
        }
        User user = findActiveUser(userId);
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "旧密码不正确");
        }
        userMapper.updatePassword(userId, passwordEncoder.encode(request.getNewPassword()));
    }

    private User findActiveUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null || user.getStatus() == UserStatus.DELETED) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        if (user.getStatus() == UserStatus.DISABLED) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "账号已禁用");
        }
        return user;
    }

    private UserProfileVO buildProfile(User user) {
        List<String> roles = roleMapper.selectByUserId(user.getId())
                .stream()
                .map(role -> role.getCode())
                .toList();
        List<String> permissions = permissionMapper.selectByUserId(user.getId())
                .stream()
                .map(permission -> permission.getCode())
                .toList();
        return UserProfileVO.from(user, roles, permissions, userMapper.selectMembershipByUserId(user.getId()));
    }
}
