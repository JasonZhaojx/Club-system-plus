package com.backend.sever.service;

import com.backend.pojo.dto.LoginDTO;
import com.backend.pojo.dto.RegisterDTO;
import com.backend.pojo.vo.AuthTokenVO;
import com.backend.pojo.vo.UserProfileVO;

public interface AuthService {
    AuthTokenVO register(RegisterDTO request);

    AuthTokenVO login(LoginDTO request);

    UserProfileVO currentUser(Long userId);
}
