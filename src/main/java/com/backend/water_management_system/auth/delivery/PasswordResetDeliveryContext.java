package com.backend.water_management_system.auth.delivery;

import com.backend.water_management_system.auth.entity.DeliveryChannel;
import com.backend.water_management_system.auth.entity.PasswordResetPurpose;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class PasswordResetDeliveryContext {

    private final UUID userId;
    private final DeliveryChannel deliveryChannel;
    private final String rawOtp;
    private final Instant otpExpiresAt;
    private final PasswordResetPurpose purpose;

    public PasswordResetDeliveryContext(
            UUID userId,
            DeliveryChannel deliveryChannel,
            String rawOtp,
            Instant otpExpiresAt,
            PasswordResetPurpose purpose
    ) {
        this.userId = Objects.requireNonNull(userId, "userId must not be null");
        this.deliveryChannel = Objects.requireNonNull(deliveryChannel, "deliveryChannel must not be null");
        this.rawOtp = Objects.requireNonNull(rawOtp, "rawOtp must not be null");
        this.otpExpiresAt = Objects.requireNonNull(otpExpiresAt, "otpExpiresAt must not be null");
        this.purpose = Objects.requireNonNull(purpose, "purpose must not be null");
    }

    public UUID getUserId() {
        return userId;
    }

    public DeliveryChannel getDeliveryChannel() {
        return deliveryChannel;
    }

    public String getRawOtp() {
        return rawOtp;
    }

    public Instant getOtpExpiresAt() {
        return otpExpiresAt;
    }

    public PasswordResetPurpose getPurpose() {
        return purpose;
    }
}