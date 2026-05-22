package com.backend.sever.config;

import com.backend.common.auth.JwtProperties;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@Profile("prod")
public class ProdSecurityStartupValidator implements ApplicationRunner {
    private final JwtProperties jwtProperties;
    private final EmailCodeProperties emailCodeProperties;

    public ProdSecurityStartupValidator(JwtProperties jwtProperties, EmailCodeProperties emailCodeProperties) {
        this.jwtProperties = jwtProperties;
        this.emailCodeProperties = emailCodeProperties;
    }

    @Override
    public void run(ApplicationArguments args) {
        requireStrongSecret("APP_JWT_SECRET", jwtProperties.secret());
        requireStrongSecret("APP_EMAIL_CODE_SECRET", emailCodeProperties.getSecret());
    }

    private void requireStrongSecret(String name, String value) {
        if (!StringUtils.hasText(value) || value.length() < 32 || value.startsWith("change-this")) {
            throw new IllegalStateException(name + " must be configured with a production secret of at least 32 characters");
        }
    }
}
