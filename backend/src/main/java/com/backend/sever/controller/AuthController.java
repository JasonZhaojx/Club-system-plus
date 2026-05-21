package com.backend.sever.controller;

import com.backend.common.auth.UserPrincipal;
import com.backend.pojo.dto.LoginDTO;
import com.backend.pojo.dto.PasswordResetCodeDTO;
import com.backend.pojo.dto.PasswordResetConfirmDTO;
import com.backend.pojo.dto.RegisterDTO;
import com.backend.pojo.vo.AuthTokenVO;
import com.backend.pojo.vo.UserProfileVO;
import com.backend.sever.common.Result;
import com.backend.sever.exception.BusinessException;
import com.backend.sever.exception.ErrorCode;
import com.backend.sever.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public Result<AuthTokenVO> register(@RequestBody RegisterDTO request) {
        return Result.success(authService.register(request));
    }

    @PostMapping("/login")
    public Result<AuthTokenVO> login(@RequestBody LoginDTO request) {
        return Result.success(authService.login(request));
    }

    @PostMapping("/password-reset/code")
    public Result<Void> sendPasswordResetCode(@RequestBody PasswordResetCodeDTO request, HttpServletRequest servletRequest) {
        authService.sendPasswordResetCode(request, clientIp(servletRequest));
        return Result.success();
    }

    @PostMapping("/password-reset/confirm")
    public Result<Void> resetPassword(@RequestBody PasswordResetConfirmDTO request) {
        authService.resetPassword(request);
        return Result.success();
    }

    @PostMapping("/logout")
    public Result<Void> logout() {
        return Result.success();
    }

    @GetMapping("/me")
    public Result<UserProfileVO> me(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return Result.success(authService.currentUser(principal.userId()));
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
