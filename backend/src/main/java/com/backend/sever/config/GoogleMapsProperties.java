package com.backend.sever.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.google-maps")
public record GoogleMapsProperties(
        String apiKey
) {
}
