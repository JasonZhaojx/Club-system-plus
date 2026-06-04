package com.backend.sever;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;
import com.backend.sever.config.BusinessRateLimitProperties;
import com.backend.sever.config.EmailCodeProperties;
import com.backend.sever.config.FileStorageProperties;
import com.backend.sever.config.AiProperties;
import com.backend.sever.config.MinioProperties;
import com.backend.sever.config.SentinelProtectionProperties;
import com.backend.sever.config.StorageProperties;

@MapperScan("com.backend.sever.mapper")
@SpringBootApplication(scanBasePackages = "com.backend")
@EnableScheduling
@EnableRabbit
@EnableConfigurationProperties({
        AiProperties.class,
        BusinessRateLimitProperties.class,
        EmailCodeProperties.class,
        FileStorageProperties.class,
        MinioProperties.class,
        SentinelProtectionProperties.class,
        StorageProperties.class
})
public class BackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(BackendApplication.class, args);
    }

}
