package com.backend.sever.service.impl;

import com.backend.common.auth.JwtProperties;
import com.backend.common.auth.JwtService;
import com.backend.pojo.dto.LoginDTO;
import com.backend.pojo.dto.PasswordResetCodeDTO;
import com.backend.pojo.dto.PasswordResetConfirmDTO;
import com.backend.pojo.dto.RegisterDTO;
import com.backend.pojo.entity.User;
import com.backend.pojo.entity.UserStatus;
import com.backend.pojo.vo.AuthTokenVO;
import com.backend.pojo.vo.UserProfileVO;
import com.backend.sever.config.SentinelResourceNames;
import com.backend.sever.exception.BusinessException;
import com.backend.sever.exception.ErrorCode;
import com.backend.sever.mapper.UserMapper;
import com.backend.sever.mapper.PermissionMapper;
import com.backend.sever.mapper.RoleMapper;
import com.backend.sever.service.AuthService;
import com.backend.sever.service.BusinessRateLimiter;
import com.backend.sever.service.EmailCodeService;
import com.backend.sever.service.SentinelGuard;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class AuthServiceImpl implements AuthService {
    private static final String DEFAULT_AVATAR_URL = "https://ts1.tc.mm.bing.net/th/id/OIP-C.4n3KcdpOWTC32-U0LjDagwHaHa?cb=thfc1falcon&rs=1&pid=ImgDetMain&o=7&rm=3";

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final BusinessRateLimiter businessRateLimiter;
    private final EmailCodeService emailCodeService;
    private final SentinelGuard sentinelGuard;

    public AuthServiceImpl(
            UserMapper userMapper,
            RoleMapper roleMapper,
            PermissionMapper permissionMapper,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            JwtProperties jwtProperties,
            BusinessRateLimiter businessRateLimiter,
            EmailCodeService emailCodeService,
            SentinelGuard sentinelGuard
    ) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.permissionMapper = permissionMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
        this.businessRateLimiter = businessRateLimiter;
        this.emailCodeService = emailCodeService;
        this.sentinelGuard = sentinelGuard;
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
        user.setAvatarUrl(DEFAULT_AVATAR_URL);
        user.setStatus(UserStatus.NORMAL);
        userMapper.insert(user);
        roleMapper.insertUserRoleByCode(user.getId(), "REGISTERED_USER");
        return buildToken(user);
    }

    @Override
    public AuthTokenVO login(LoginDTO request, String ipAddress) {
        validateLogin(request);
        String username = request.getUsername().trim();
        businessRateLimiter.checkLogin(username, ipAddress);
        User user = userMapper.selectByUsername(username);
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户名或密码错误");
        }
        ensureActive(user);
        return buildToken(user);
    }

    @Override
    public void sendPasswordResetCode(PasswordResetCodeDTO request, String ipAddress) {
        String email = normalizeEmail(request == null ? null : request.getEmail());
        businessRateLimiter.checkPasswordResetEmail(email, ipAddress);
        try (SentinelGuard.GuardEntry guard = sentinelGuard.enter(SentinelResourceNames.EMAIL_PASSWORD_RESET_CODE, email)) {
            try {
                User user = userMapper.selectByEmail(email);
                if (user != null && user.getStatus() == UserStatus.NORMAL) {
                    emailCodeService.createAndSendPasswordResetCode(email);
                }
            } catch (RuntimeException exception) {
                guard.trace(exception);
                throw exception;
            }
        }
    }

    @Override
    @Transactional
    public void resetPassword(PasswordResetConfirmDTO request) {
        String email = normalizeEmail(request == null ? null : request.getEmail());
        validateResetPassword(request);
        emailCodeService.verifyPasswordResetCode(email, request.getCode());
        User user = userMapper.selectByEmail(email);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "账号不存在");
        }
        ensureActive(user);
        userMapper.updatePasswordByEmail(email, passwordEncoder.encode(request.getNewPassword()));
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

    private void validateResetPassword(PasswordResetConfirmDTO request) {
        if (request == null
                || !StringUtils.hasText(request.getCode())
                || !StringUtils.hasText(request.getNewPassword())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "邮箱、验证码和新密码不能为空");
        }
        if (request.getNewPassword().length() < 8) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "密码长度不能少于 8 个字符");
        }
    }

    private String normalizeEmail(String email) {
        if (!StringUtils.hasText(email)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "邮箱不能为空");
        }
        String normalized = email.trim().toLowerCase();
        if (normalized.length() > 120 || !normalized.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "邮箱格式不正确");
        }
        return normalized;
    }
}
