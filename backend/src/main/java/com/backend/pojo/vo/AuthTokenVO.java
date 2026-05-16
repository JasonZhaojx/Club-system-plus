package com.backend.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthTokenVO {
    private String accessToken;
    private String tokenType;
    private long expiresIn;
    private UserProfileVO user;
}
