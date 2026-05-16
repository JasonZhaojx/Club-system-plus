package com.backend.sever.service.impl;

import com.backend.common.auth.JwtProperties;
import com.backend.common.auth.JwtService;
import com.backend.pojo.dto.LoginDTO;
import com.backend.pojo.dto.RegisterDTO;
import com.backend.pojo.entity.User;
import com.backend.pojo.entity.UserStatus;
import com.backend.pojo.vo.AuthTokenVO;
import com.backend.pojo.vo.UserProfileVO;
import com.backend.sever.exception.BusinessException;
import com.backend.sever.exception.ErrorCode;
import com.backend.sever.mapper.UserMapper;
import com.backend.sever.mapper.PermissionMapper;
import com.backend.sever.mapper.RoleMapper;
import com.backend.sever.service.AuthService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class AuthServiceImpl implements AuthService {
    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;

    public AuthServiceImpl(
            UserMapper userMapper,
            RoleMapper roleMapper,
            PermissionMapper permissionMapper,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            JwtProperties jwtProperties
    ) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.permissionMapper = permissionMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
    }

    @Override
    @Transactional
    public AuthTokenVO register(RegisterDTO request) {
        validateRegister(request);
        String username = request.getUsername().trim();
        if (userMapper.countByUsername(username) > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "用户名已存在");
        }

        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setNickname(StringUtils.hasText(request.getNickname()) ? request.getNickname().trim() : username);
        user.setEmail(StringUtils.hasText(request.getEmail()) ? request.getEmail().trim() : null);
        user.setStatus(UserStatus.NORMAL);
        userMapper.insert(user);
        roleMapper.insertUserRoleByCode(user.getId(), "REGISTERED_USER");
        return buildToken(user);
    }

    @Override
    public AuthTokenVO login(LoginDTO request) {
        validateLogin(request);
        User user = userMapper.selectByUsername(request.getUsername().trim());
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户名或密码错误");
        }
        ensureActive(user);
        return buildToken(user);
    }

    @Override
    public UserProfileVO currentUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        ensureActive(user);
        return buildProfile(user);
    }

    private AuthTokenVO buildToken(User user) {
        return new AuthTokenVO(
                jwtService.generate(user),
                "Bearer",
                jwtProperties.expirationSeconds(),
                buildProfile(user)
        );
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

    private void ensureActive(User user) {
        if (user.getStatus() == UserStatus.DISABLED) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "账号已禁用");
        }
        if (user.getStatus() == UserStatus.DELETED) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "账号不存在");
        }
    }

    private void validateRegister(RegisterDTO request) {
        if (request == null
                || !StringUtils.hasText(request.getUsername())
                || !StringUtils.hasText(request.getPassword())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "用户名和密码不能为空");
        }
        if (request.getUsername().trim().length() < 3 || request.getUsername().trim().length() > 50) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "用户名长度必须为 3 到 50 个字符");
        }
        if (request.getPassword().length() < 8) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "密码长度不能少于 8 个字符");
        }
    }

    private void validateLogin(LoginDTO request) {
        if (request == null
                || !StringUtils.hasText(request.getUsername())
                || !StringUtils.hasText(request.getPassword())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "用户名和密码不能为空");
        }
    }
}
