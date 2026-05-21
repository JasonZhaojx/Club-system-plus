package com.backend.pojo.dto;

import lombok.Data;

@Data
public class PasswordResetConfirmDTO {
    private String email;
    private String code;
    private String newPassword;
}
