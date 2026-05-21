package com.backend.sever.service.impl;

import com.backend.sever.config.EmailCodeProperties;
import com.backend.sever.service.MailService;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SmtpMailService implements MailService {
    private final JavaMailSender mailSender;
    private final EmailCodeProperties properties;

    public SmtpMailService(JavaMailSender mailSender, EmailCodeProperties properties) {
        this.mailSender = mailSender;
        this.properties = properties;
    }

    @Override
    public void sendPasswordResetCode(String email, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        if (StringUtils.hasText(properties.getFrom())) {
            message.setFrom(properties.getFrom());
        }
        message.setTo(email);
        message.setSubject(properties.getSubject());
        message.setText("""
                Your password reset code is: %s

                The code expires in 10 minutes. If you did not request this, ignore this email.
                """.formatted(code));
        mailSender.send(message);
    }
}
