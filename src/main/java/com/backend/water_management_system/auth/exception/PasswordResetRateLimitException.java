package com.backend.water_management_system.auth.exception;

public class PasswordResetRateLimitException extends RuntimeException {

    public PasswordResetRateLimitException() {
        super("Too many requests. Please try again later.");
    }
}
