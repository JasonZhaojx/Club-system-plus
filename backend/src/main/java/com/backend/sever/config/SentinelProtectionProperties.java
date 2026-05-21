package com.backend.sever.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.sentinel")
public class SentinelProtectionProperties {
    private boolean enabled = true;
    private double couponClaimQps = 80;
    private double couponClaimHotBatchQps = 30;
    private double activityRegisterQps = 80;
    private double activityRegisterHotActivityQps = 30;
    private double emailCodeQps = 20;
    private double emailCodeHotEmailQps = 3;
    private double exceptionRatio = 0.5;
    private int minRequestAmount = 20;
    private int statIntervalMs = 10000;
    private int timeWindowSeconds = 10;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public double getCouponClaimQps() {
        return couponClaimQps;
    }

    public void setCouponClaimQps(double couponClaimQps) {
        this.couponClaimQps = couponClaimQps;
    }

    public double getCouponClaimHotBatchQps() {
        return couponClaimHotBatchQps;
    }

    public void setCouponClaimHotBatchQps(double couponClaimHotBatchQps) {
        this.couponClaimHotBatchQps = couponClaimHotBatchQps;
    }

    public double getActivityRegisterQps() {
        return activityRegisterQps;
    }

    public void setActivityRegisterQps(double activityRegisterQps) {
        this.activityRegisterQps = activityRegisterQps;
    }

    public double getActivityRegisterHotActivityQps() {
        return activityRegisterHotActivityQps;
    }

    public void setActivityRegisterHotActivityQps(double activityRegisterHotActivityQps) {
        this.activityRegisterHotActivityQps = activityRegisterHotActivityQps;
    }

    public double getEmailCodeQps() {
        return emailCodeQps;
    }

    public void setEmailCodeQps(double emailCodeQps) {
        this.emailCodeQps = emailCodeQps;
    }

    public double getEmailCodeHotEmailQps() {
        return emailCodeHotEmailQps;
    }

    public void setEmailCodeHotEmailQps(double emailCodeHotEmailQps) {
        this.emailCodeHotEmailQps = emailCodeHotEmailQps;
    }

    public double getExceptionRatio() {
        return exceptionRatio;
    }

    public void setExceptionRatio(double exceptionRatio) {
        this.exceptionRatio = exceptionRatio;
    }

    public int getMinRequestAmount() {
        return minRequestAmount;
    }

    public void setMinRequestAmount(int minRequestAmount) {
        this.minRequestAmount = minRequestAmount;
    }

    public int getStatIntervalMs() {
        return statIntervalMs;
    }

    public void setStatIntervalMs(int statIntervalMs) {
        this.statIntervalMs = statIntervalMs;
    }

    public int getTimeWindowSeconds() {
        return timeWindowSeconds;
    }

    public void setTimeWindowSeconds(int timeWindowSeconds) {
        this.timeWindowSeconds = timeWindowSeconds;
    }
}
