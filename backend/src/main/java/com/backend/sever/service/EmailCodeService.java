package com.backend.sever.service;

public interface EmailCodeService {
    void createAndSendPasswordResetCode(String email);

    void verifyPasswordResetCode(String email, String code);
}
