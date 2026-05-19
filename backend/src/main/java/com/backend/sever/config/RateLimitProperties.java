package com.backend.sever.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.rate-limit")
public class RateLimitProperties {
    private boolean enabled = true;
    private int capacity = 120;
    private int refillPerSecond = 60;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public int getRefillPerSecond() {
        return refillPerSecond;
    }

    public void setRefillPerSecond(int refillPerSecond) {
        this.refillPerSecond = refillPerSecond;
    }
}
