package com.backend.water_management_system.auth.entity;

import com.backend.water_management_system.user.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "password_reset_challenges",
        indexes = {
                @Index(name = "idx_prc_user_purpose", columnList = "user_id, purpose"),
                @Index(name = "idx_prc_expires_at", columnList = "expires_at"),
                @Index(name = "idx_prc_active_lookup", columnList = "user_id, purpose, used_at, invalidated_at, expires_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordResetChallenge {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", nullable = false, length = 32)
    private PasswordResetPurpose purpose;

    @Column(name = "otp_hmac", nullable = false, length = 128)
    private String otpHmac;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_channel", nullable = false, length = 32)
    private DeliveryChannel deliveryChannel;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "invalidated_at")
    private Instant invalidatedAt;

    @Column(name = "failed_attempts", nullable = false)
    @Builder.Default
    private int failedAttempts = 0;

    @Column(name = "resend_available_at", nullable = false)
    private Instant resendAvailableAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Version
    @Column(name = "version")
    private Long version;

    @PrePersist
    protected void onCreate() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        this.createdAt = Instant.now();
    }
}
