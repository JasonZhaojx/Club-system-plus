package com.backend.sever;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;
import com.backend.sever.config.BusinessRateLimitProperties;
import com.backend.sever.config.EmailCodeProperties;
import com.backend.sever.config.SentinelProtectionProperties;

@MapperScan("com.backend.sever.mapper")
@SpringBootApplication(scanBasePackages = "com.backend")
@EnableScheduling
@EnableRabbit
@EnableConfigurationProperties({
        BusinessRateLimitProperties.class,
        EmailCodeProperties.class,
        SentinelProtectionProperties.class
})
public class BackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(BackendApplication.class, args);
    }

}
