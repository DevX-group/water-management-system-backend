package com.backend.water_management_system.auth.service;

import com.backend.water_management_system.auth.config.PasswordResetProperties;
import com.backend.water_management_system.auth.delivery.PasswordResetDeliveryContext;
import com.backend.water_management_system.auth.delivery.PasswordResetDeliveryService;
import com.backend.water_management_system.auth.dto.PasswordResetCompleteRequest;
import com.backend.water_management_system.auth.dto.PasswordResetRequest;
import com.backend.water_management_system.auth.dto.PasswordResetVerifyRequest;
import com.backend.water_management_system.auth.entity.DeliveryChannel;
import com.backend.water_management_system.auth.entity.PasswordResetAuthorization;
import com.backend.water_management_system.auth.entity.PasswordResetChallenge;
import com.backend.water_management_system.auth.entity.PasswordResetPurpose;
import com.backend.water_management_system.auth.exception.PasswordResetException;
import com.backend.water_management_system.auth.exception.PasswordResetRateLimitException;
import com.backend.water_management_system.auth.rate.PasswordResetRateLimiter;
import com.backend.water_management_system.auth.repository.PasswordResetAuthorizationRepository;
import com.backend.water_management_system.auth.repository.PasswordResetChallengeRepository;
import com.backend.water_management_system.auth.security.PasswordResetCryptography;
import com.backend.water_management_system.user.entity.User;
import com.backend.water_management_system.user.enums.UserStatus;
import com.backend.water_management_system.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

@Service
public class PasswordResetService {

    public static final String REQUEST_MESSAGE =
            "If an account exists, a verification code has been sent to the registered recovery contact.";

    public static final String INVALID_CODE_MESSAGE =
            "This verification code is invalid or has expired.";

    private final UserRepository userRepository;
    private final PasswordResetChallengeRepository challengeRepository;
    private final PasswordResetAuthorizationRepository authorizationRepository;
    private final PasswordResetProperties properties;
    private final PasswordResetCryptography cryptography;
    private final PasswordResetRateLimiter rateLimiter;
    private final List<PasswordResetDeliveryService> deliveryServices;
    private final PasswordEncoder passwordEncoder;
    private final Executor deliveryExecutor;
    private final SecureRandom secureRandom = new SecureRandom();

    public PasswordResetService(
            UserRepository userRepository,
            PasswordResetChallengeRepository challengeRepository,
            PasswordResetAuthorizationRepository authorizationRepository,
            PasswordResetProperties properties,
            PasswordResetCryptography cryptography,
            PasswordResetRateLimiter rateLimiter,
            List<PasswordResetDeliveryService> deliveryServices,
            PasswordEncoder passwordEncoder,
            @Qualifier("passwordResetDeliveryExecutor")
            Executor deliveryExecutor
    ) {
        this.userRepository = userRepository;
        this.challengeRepository = challengeRepository;
        this.authorizationRepository = authorizationRepository;
        this.properties = properties;
        this.cryptography = cryptography;
        this.rateLimiter = rateLimiter;
        this.deliveryServices = deliveryServices;
        this.passwordEncoder = passwordEncoder;
        this.deliveryExecutor = deliveryExecutor;
    }

    @Transactional
    public Map<String, String> request(
            PasswordResetRequest request,
            String clientIp
    ) {
        String normalizedNic = normalizeNic(request.nic());
        enforceRequestLimits(normalizedNic, clientIp);

        User user = userRepository.findLockedByNic(normalizedNic).orElse(null);

        if (user == null) {
            performDummyWork(normalizedNic);
            return Map.of("message", REQUEST_MESSAGE);
        }

        if (user.getStatus() != UserStatus.ACTIVE
                || user.getEmail() == null
                || user.getEmail().isBlank()) {
            performDummyWork(normalizedNic);
            return Map.of("message", REQUEST_MESSAGE);
        }

        Instant now = Instant.now();

        PasswordResetChallenge active = challengeRepository
                .findFirstByUserAndPurposeAndUsedAtIsNullAndInvalidatedAtIsNullAndExpiresAtAfterOrderByCreatedAtDesc(
                        user,
                        PasswordResetPurpose.PASSWORD_RESET,
                        now
                )
                .orElse(null);

        if (active != null && active.getResendAvailableAt().isAfter(now)) {
            return Map.of("message", REQUEST_MESSAGE);
        }

        challengeRepository.invalidateActiveByUserAndPurpose(
                user,
                PasswordResetPurpose.PASSWORD_RESET,
                now
        );

        String otp = sixDigitOtp();
        UUID challengeId = UUID.randomUUID();

        Instant expiresAt = now.plus(
                Duration.ofMinutes(properties.getOtpExpiryMinutes())
        );

        PasswordResetChallenge challenge = PasswordResetChallenge.builder()
                .id(challengeId)
                .user(user)
                .purpose(PasswordResetPurpose.PASSWORD_RESET)
                .otpHmac(
                        cryptography.otpHmac(
                                challengeId.toString(),
                                user.getId().toString(),
                                PasswordResetPurpose.PASSWORD_RESET.name(),
                                otp
                        )
                )
                .deliveryChannel(DeliveryChannel.EMAIL)
                .expiresAt(expiresAt)
                .resendAvailableAt(
                        now.plusSeconds(properties.getResendCooldownSeconds())
                )
                .build();

        challengeRepository.save(challenge);

        PasswordResetDeliveryContext context =
                new PasswordResetDeliveryContext(
                        user.getId(),
                        DeliveryChannel.EMAIL,
                        otp,
                        expiresAt,
                        PasswordResetPurpose.PASSWORD_RESET
                );

        dispatchAfterCommit(context);

        return Map.of("message", REQUEST_MESSAGE);
    }

    private void dispatchAfterCommit(
            PasswordResetDeliveryContext context
    ) {
        Runnable dispatch = () -> {
            try {
                deliveryServices.stream()
                        .filter(service ->
                                service.supports(DeliveryChannel.EMAIL))
                        .findFirst()
                        .ifPresent(service -> service.deliver(context));
            } catch (RuntimeException ignored) {
                // Delivery failure must not reveal account state through the API.
            }
        };

        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            submitDelivery(dispatch);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        submitDelivery(dispatch);
                    }
                }
        );
    }

    private void submitDelivery(Runnable dispatch) {
        try {
            deliveryExecutor.execute(dispatch);
        } catch (RejectedExecutionException ignored) {
            // A saturated in-process queue must not alter the public response.
        }
    }

    @Transactional
    public Map<String, String> verify(
            PasswordResetVerifyRequest request,
            String clientIp
    ) {
        enforceVerificationLimit(clientIp);

        User user = userRepository
                .findByNic(normalizeNic(request.nic()))
                .orElseThrow(
                        () -> new PasswordResetException(
                                INVALID_CODE_MESSAGE
                        )
                );

        Instant now = Instant.now();

        PasswordResetChallenge challenge = challengeRepository
                .findFirstByUserAndPurposeAndUsedAtIsNullAndInvalidatedAtIsNullAndExpiresAtAfterOrderByCreatedAtDesc(
                        user,
                        PasswordResetPurpose.PASSWORD_RESET,
                        now
                )
                .orElseThrow(
                        () -> new PasswordResetException(
                                INVALID_CODE_MESSAGE
                        )
                );

        if (challenge.getFailedAttempts()
                >= properties.getMaxOtpAttempts()) {
            throw new PasswordResetException(INVALID_CODE_MESSAGE);
        }

        String submittedHmac = cryptography.otpHmac(
                challenge.getId().toString(),
                user.getId().toString(),
                challenge.getPurpose().name(),
                request.otp()
        );

        if (!cryptography.matches(
                challenge.getOtpHmac(),
                submittedHmac
        )) {
            challenge.setFailedAttempts(
                    challenge.getFailedAttempts() + 1
            );

            if (challenge.getFailedAttempts()
                    >= properties.getMaxOtpAttempts()) {
                challenge.setInvalidatedAt(now);
            }

            challengeRepository.save(challenge);

            throw new PasswordResetException(INVALID_CODE_MESSAGE);
        }

        challenge.setUsedAt(now);
        challengeRepository.save(challenge);

        byte[] authorizationBytes = new byte[32];
        secureRandom.nextBytes(authorizationBytes);

        String authorization = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(authorizationBytes);

        authorizationRepository.save(
                PasswordResetAuthorization.builder()
                        .user(user)
                        .authorizationDigest(
                                cryptography.authorizationDigest(
                                        authorization
                                )
                        )
                        .expiresAt(
                                now.plus(
                                        Duration.ofMinutes(
                                                properties
                                                        .getAuthorizationExpiryMinutes()
                                        )
                                )
                        )
                        .build()
        );

        return Map.of("resetAuthorization", authorization);
    }

    @Transactional
    public Map<String, String> complete(
            PasswordResetCompleteRequest request
    ) {
        if (!request.newPassword().equals(
                request.confirmPassword()
        )) {
            throw new PasswordResetException(
                    "Passwords do not match"
            );
        }

        PasswordResetAuthorization authorization =
                authorizationRepository
                        .findByAuthorizationDigest(
                                cryptography.authorizationDigest(
                                        request.resetAuthorization()
                                )
                        )
                        .orElseThrow(
                                () -> new PasswordResetException(
                                        "Reset authorization is invalid or has expired."
                                )
                        );

        Instant now = Instant.now();

        if (authorization.getUsedAt() != null
                || !authorization.getExpiresAt().isAfter(now)) {
            throw new PasswordResetException(
                    "Reset authorization is invalid or has expired."
            );
        }

        if (request.newPassword().length() < 8) {
            throw new PasswordResetException(
                    "Password must be at least 8 characters"
            );
        }

        User user = userRepository
                .findLockedById(authorization.getUser().getId())
                .orElseThrow(
                        () -> new PasswordResetException(
                                "Reset authorization is invalid or has expired."
                        )
                );

        user.setPasswordHash(
                passwordEncoder.encode(request.newPassword())
        );

        user.setTokenVersion(
                Math.addExact(user.getTokenVersion(), 1L)
        );

        authorization.setUsedAt(now);

        userRepository.save(user);
        authorizationRepository.save(authorization);

        return Map.of(
                "message",
                "Password reset successfully. You can now log in."
        );
    }

    private void enforceRequestLimits(
            String normalizedNic,
            String clientIp
    ) {
        Duration window = Duration.ofMinutes(
                properties.getRateLimitWindowMinutes()
        );

        boolean ipAllowed = rateLimiter.tryAcquire(
                "ip:request:" + safeIp(clientIp),
                properties.getIpRequestsPerWindow(),
                window
        );

        boolean nicAllowed = rateLimiter.tryAcquire(
                "nic:request:"
                        + cryptography.nicRateLimitDigest(normalizedNic),
                properties.getNicRequestsPerWindow(),
                window
        );

        if (!ipAllowed || !nicAllowed) {
            throw new PasswordResetRateLimitException();
        }
    }

    private void enforceVerificationLimit(String clientIp) {
        boolean allowed = rateLimiter.tryAcquire(
                "ip:verify:" + safeIp(clientIp),
                properties.getVerificationRequestsPerWindow(),
                Duration.ofMinutes(
                        properties.getRateLimitWindowMinutes()
                )
        );

        if (!allowed) {
            throw new PasswordResetRateLimitException();
        }
    }

    private void performDummyWork(String normalizedNic) {
        cryptography.otpHmac(
                "dummy",
                "dummy",
                PasswordResetPurpose.PASSWORD_RESET.name(),
                normalizedNic
        );
    }

    private String sixDigitOtp() {
        return String.format(
                "%06d",
                secureRandom.nextInt(1_000_000)
        );
    }

    private String normalizeNic(String nic) {
        return nic == null
                ? ""
                : nic.trim().toUpperCase(java.util.Locale.ROOT);
    }

    private String safeIp(String clientIp) {
        return clientIp == null || clientIp.isBlank()
                ? "unknown"
                : clientIp;
    }
}