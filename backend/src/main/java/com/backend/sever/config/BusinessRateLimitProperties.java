package com.backend.sever.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.business-rate-limit")
public class BusinessRateLimitProperties {
    private boolean enabled = true;
    private int couponClaimLimit = 1;
    private int couponClaimWindowSeconds = 3;
    private int activityRegisterLimit = 1;
    private int activityRegisterWindowSeconds = 3;
    private int emailCooldownLimit = 1;
    private int emailCooldownWindowSeconds = 60;
    private int emailHourlyLimit = 5;
    private int emailHourlyWindowSeconds = 3600;
    private int emailIpHourlyLimit = 20;
    private int emailIpHourlyWindowSeconds = 3600;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getCouponClaimLimit() {
        return couponClaimLimit;
    }

    public void setCouponClaimLimit(int couponClaimLimit) {
        this.couponClaimLimit = couponClaimLimit;
    }

    public int getCouponClaimWindowSeconds() {
        return couponClaimWindowSeconds;
    }

    public void setCouponClaimWindowSeconds(int couponClaimWindowSeconds) {
        this.couponClaimWindowSeconds = couponClaimWindowSeconds;
    }

    public int getActivityRegisterLimit() {
        return activityRegisterLimit;
    }

    public void setActivityRegisterLimit(int activityRegisterLimit) {
        this.activityRegisterLimit = activityRegisterLimit;
    }

    public int getActivityRegisterWindowSeconds() {
        return activityRegisterWindowSeconds;
    }

    public void setActivityRegisterWindowSeconds(int activityRegisterWindowSeconds) {
        this.activityRegisterWindowSeconds = activityRegisterWindowSeconds;
    }

    public int getEmailCooldownLimit() {
        return emailCooldownLimit;
    }

    public void setEmailCooldownLimit(int emailCooldownLimit) {
        this.emailCooldownLimit = emailCooldownLimit;
    }

    public int getEmailCooldownWindowSeconds() {
        return emailCooldownWindowSeconds;
    }

    public void setEmailCooldownWindowSeconds(int emailCooldownWindowSeconds) {
        this.emailCooldownWindowSeconds = emailCooldownWindowSeconds;
    }

    public int getEmailHourlyLimit() {
        return emailHourlyLimit;
    }

    public void setEmailHourlyLimit(int emailHourlyLimit) {
        this.emailHourlyLimit = emailHourlyLimit;
    }

    public int getEmailHourlyWindowSeconds() {
        return emailHourlyWindowSeconds;
    }

    public void setEmailHourlyWindowSeconds(int emailHourlyWindowSeconds) {
        this.emailHourlyWindowSeconds = emailHourlyWindowSeconds;
    }

    public int getEmailIpHourlyLimit() {
        return emailIpHourlyLimit;
    }

    public void setEmailIpHourlyLimit(int emailIpHourlyLimit) {
        this.emailIpHourlyLimit = emailIpHourlyLimit;
    }

    public int getEmailIpHourlyWindowSeconds() {
        return emailIpHourlyWindowSeconds;
    }

    public void setEmailIpHourlyWindowSeconds(int emailIpHourlyWindowSeconds) {
        this.emailIpHourlyWindowSeconds = emailIpHourlyWindowSeconds;
    }
}
