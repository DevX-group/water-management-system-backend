package com.backend.water_management_system.auth.rate;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class BoundedExpiringPasswordResetRateLimiter implements PasswordResetRateLimiter {

    private static final int MAX_BUCKETS = 10_000;
    private final Map<String, Bucket> buckets = new LinkedHashMap<>();

    @Override
    public synchronized boolean tryAcquire(String key, int limit, Duration window) {
        Instant now = Instant.now();
        prune(now);
        Bucket bucket = buckets.get(key);
        if (bucket == null || !now.isBefore(bucket.expiresAt)) {
            bucket = new Bucket(1, now.plus(window));
            buckets.put(key, bucket);
            return true;
        }
        if (bucket.count >= limit) {
            return false;
        }
        bucket.count++;
        return true;
    }

    private void prune(Instant now) {
        Iterator<Map.Entry<String, Bucket>> iterator = buckets.entrySet().iterator();
        while (iterator.hasNext()) {
            if (!now.isBefore(iterator.next().getValue().expiresAt)) {
                iterator.remove();
            }
        }
        while (buckets.size() >= MAX_BUCKETS) {
            buckets.remove(buckets.keySet().iterator().next());
        }
    }

    private static final class Bucket {
        private int count;
        private final Instant expiresAt;

        private Bucket(int count, Instant expiresAt) {
            this.count = count;
            this.expiresAt = expiresAt;
        }
    }
}
