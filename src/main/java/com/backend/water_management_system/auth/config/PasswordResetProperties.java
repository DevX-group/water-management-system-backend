package com.backend.water_management_system.auth.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@Validated
@ConfigurationProperties(prefix = "app.password-reset")
public class PasswordResetProperties {

    @Min(1)
    private int otpExpiryMinutes = 5;

    @Min(1)
    private int maxOtpAttempts = 5;

    @Min(1)
    private int resendCooldownSeconds = 60;

    @Min(1)
    private int authorizationExpiryMinutes = 10;

    @NotBlank
    private String otpHmacSecret;

    @Min(1)
    private int nicRequestsPerWindow = 3;

    @Min(1)
    private int ipRequestsPerWindow = 20;

    @Min(1)
    private int verificationRequestsPerWindow = 30;

    @Min(1)
    private int rateLimitWindowMinutes = 15;

    public int getOtpExpiryMinutes() {
        return otpExpiryMinutes;
    }

    public void setOtpExpiryMinutes(int otpExpiryMinutes) {
        this.otpExpiryMinutes = otpExpiryMinutes;
    }

    public int getMaxOtpAttempts() {
        return maxOtpAttempts;
    }

    public void setMaxOtpAttempts(int maxOtpAttempts) {
        this.maxOtpAttempts = maxOtpAttempts;
    }

    public int getResendCooldownSeconds() {
        return resendCooldownSeconds;
    }

    public void setResendCooldownSeconds(int resendCooldownSeconds) {
        this.resendCooldownSeconds = resendCooldownSeconds;
    }

    public int getAuthorizationExpiryMinutes() {
        return authorizationExpiryMinutes;
    }

    public void setAuthorizationExpiryMinutes(int authorizationExpiryMinutes) {
        this.authorizationExpiryMinutes = authorizationExpiryMinutes;
    }

    public String getOtpHmacSecret() {
        return otpHmacSecret;
    }

    public void setOtpHmacSecret(String otpHmacSecret) {
        this.otpHmacSecret = otpHmacSecret;
    }

    public int getNicRequestsPerWindow() {
        return nicRequestsPerWindow;
    }

    public void setNicRequestsPerWindow(int nicRequestsPerWindow) {
        this.nicRequestsPerWindow = nicRequestsPerWindow;
    }

    public int getIpRequestsPerWindow() {
        return ipRequestsPerWindow;
    }

    public void setIpRequestsPerWindow(int ipRequestsPerWindow) {
        this.ipRequestsPerWindow = ipRequestsPerWindow;
    }

    public int getVerificationRequestsPerWindow() {
        return verificationRequestsPerWindow;
    }

    public void setVerificationRequestsPerWindow(int verificationRequestsPerWindow) {
        this.verificationRequestsPerWindow = verificationRequestsPerWindow;
    }

    public int getRateLimitWindowMinutes() {
        return rateLimitWindowMinutes;
    }

    public void setRateLimitWindowMinutes(int rateLimitWindowMinutes) {
        this.rateLimitWindowMinutes = rateLimitWindowMinutes;
    }
}
