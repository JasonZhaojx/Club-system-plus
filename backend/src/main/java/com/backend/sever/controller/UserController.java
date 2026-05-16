package com.backend.sever.controller;

import com.backend.common.auth.UserPrincipal;
import com.backend.pojo.dto.ChangePasswordDTO;
import com.backend.pojo.dto.UpdateProfileDTO;
import com.backend.pojo.vo.UserProfileVO;
import com.backend.sever.common.Result;
import com.backend.sever.exception.BusinessException;
import com.backend.sever.exception.ErrorCode;
import com.backend.sever.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users/me")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public Result<UserProfileVO> profile(Authentication authentication) {
        return Result.success(userService.currentUser(currentUserId(authentication)));
    }

    @PatchMapping
    public Result<UserProfileVO> updateProfile(
            Authentication authentication,
            @RequestBody UpdateProfileDTO request
    ) {
        return Result.success(userService.updateProfile(currentUserId(authentication), request));
    }

    @PutMapping("/password")
    public Result<Void> changePassword(Authentication authentication, @RequestBody ChangePasswordDTO request) {
        userService.changePassword(currentUserId(authentication), request);
        return Result.success();
    }

    private Long currentUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return principal.userId();
    }
}
