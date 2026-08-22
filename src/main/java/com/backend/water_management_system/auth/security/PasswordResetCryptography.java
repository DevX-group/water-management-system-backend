package com.backend.water_management_system.auth.security;

import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

@Component
public class PasswordResetCryptography {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private final byte[] secret;

    public PasswordResetCryptography(com.backend.water_management_system.auth.config.PasswordResetProperties properties) {
        this.secret = properties.getOtpHmacSecret().getBytes(StandardCharsets.UTF_8);
    }

    public String otpHmac(String challengeId, String userId, String purpose, String otp) {
        return hmac("password-reset:otp:" + challengeId + ":" + userId + ":" + purpose + ":" + otp);
    }

    public String nicRateLimitDigest(String normalizedNic) {
        return hmac("rate-limit:nic:" + normalizedNic);
    }

    public String authorizationDigest(String authorization) {
        return HexFormat.of().formatHex(sha256(authorization.getBytes(StandardCharsets.UTF_8)));
    }

    public boolean matches(String expected, String actual) {
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8));
    }

    private String hmac(String value) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret, HMAC_ALGORITHM));
            return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Password-reset cryptography is unavailable", exception);
        }
    }

    private byte[] sha256(byte[] value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value);
        } catch (Exception exception) {
            throw new IllegalStateException("Password-reset digest is unavailable", exception);
        }
    }
}
