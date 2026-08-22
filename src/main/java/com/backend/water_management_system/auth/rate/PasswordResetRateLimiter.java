package com.backend.water_management_system.auth.rate;

import java.time.Duration;

public interface PasswordResetRateLimiter {

    boolean tryAcquire(String key, int limit, Duration window);
}
