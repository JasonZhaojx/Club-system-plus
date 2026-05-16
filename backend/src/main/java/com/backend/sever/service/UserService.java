package com.backend.sever.service;

import com.backend.pojo.dto.ChangePasswordDTO;
import com.backend.pojo.dto.UpdateProfileDTO;
import com.backend.pojo.vo.UserProfileVO;

public interface UserService {
    UserProfileVO currentUser(Long userId);

    UserProfileVO updateProfile(Long userId, UpdateProfileDTO request);

    void changePassword(Long userId, ChangePasswordDTO request);
}
