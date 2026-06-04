package com.backend.sever.config;

import com.backend.common.auth.JwtProperties;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@Profile("prod")
public class ProdSecurityStartupValidator implements ApplicationRunner {
    private final JwtProperties jwtProperties;
    private final EmailCodeProperties emailCodeProperties;
    private final MinioProperties minioProperties;
    private final Environment environment;

    public ProdSecurityStartupValidator(
            JwtProperties jwtProperties,
            EmailCodeProperties emailCodeProperties,
            MinioProperties minioProperties,
            Environment environment
    ) {
        this.jwtProperties = jwtProperties;
        this.emailCodeProperties = emailCodeProperties;
        this.minioProperties = minioProperties;
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        requireStrongSecret("APP_JWT_SECRET", jwtProperties.secret(), 32);
        requireStrongSecret("APP_EMAIL_CODE_SECRET", emailCodeProperties.getSecret(), 32);
        requireConfigured("MYSQL_USERNAME", environment.getProperty("spring.datasource.username"));
        requireStrongSecret("MYSQL_PASSWORD", environment.getProperty("spring.datasource.password"), 12);
        requireStrongSecret("REDIS_PASSWORD", environment.getProperty("spring.data.redis.password"), 12);
        requireConfigured("RABBITMQ_USERNAME", environment.getProperty("spring.rabbitmq.username"));
        requireStrongSecret("RABBITMQ_PASSWORD", environment.getProperty("spring.rabbitmq.password"), 12);
        requireConfigured("MINIO_ROOT_USER", minioProperties.getAccessKey());
        requireStrongSecret("MINIO_ROOT_PASSWORD", minioProperties.getSecretKey(), 12);
    }

    private void requireStrongSecret(String name, String value, int minLength) {
        if (!StringUtils.hasText(value)
                || value.length() < minLength
                || value.startsWith("change-this")
                || "guest".equals(value)
                || "minioadmin".equals(value)) {
            throw new IllegalStateException(name + " must be configured with a non-default production secret of at least " + minLength + " characters");
        }
    }

    private void requireConfigured(String name, String value) {
        if (!StringUtils.hasText(value)
                || "root".equals(value)
                || "guest".equals(value)
                || "minioadmin".equals(value)) {
            throw new IllegalStateException(name + " must be configured with a non-default production value");
        }
    }
}
