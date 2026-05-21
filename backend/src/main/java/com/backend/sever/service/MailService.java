package com.backend.sever.service;

public interface MailService {
    void sendPasswordResetCode(String email, String code);
}
