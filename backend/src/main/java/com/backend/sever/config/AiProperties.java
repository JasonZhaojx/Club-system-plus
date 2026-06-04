package com.backend.sever.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ai")
public record AiProperties(
        boolean enabled,
        String baseUrl,
        String apiKey,
        String model,
        double temperature,
        int maxTokens
) {
}
