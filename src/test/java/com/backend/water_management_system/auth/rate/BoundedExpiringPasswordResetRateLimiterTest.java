package com.backend.water_management_system.auth.rate;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class BoundedExpiringPasswordResetRateLimiterTest {

    @Test
    void enforcesConfiguredLimitAndExpiresEntries() throws InterruptedException {
        BoundedExpiringPasswordResetRateLimiter limiter = new BoundedExpiringPasswordResetRateLimiter();

        assertThat(limiter.tryAcquire("ip:test", 3, Duration.ofMillis(20))).isTrue();
        assertThat(limiter.tryAcquire("ip:test", 3, Duration.ofMillis(20))).isTrue();
        assertThat(limiter.tryAcquire("ip:test", 3, Duration.ofMillis(20))).isTrue();
        assertThat(limiter.tryAcquire("ip:test", 3, Duration.ofMillis(20))).isFalse();
        Thread.sleep(30);
        assertThat(limiter.tryAcquire("ip:test", 3, Duration.ofMillis(20))).isTrue();
    }

    @Test
    void remainsBoundedAtTenThousandBuckets() throws Exception {
        BoundedExpiringPasswordResetRateLimiter limiter = new BoundedExpiringPasswordResetRateLimiter();
        for (int index = 0; index < 10_001; index++) {
            assertThat(limiter.tryAcquire("key-" + index, 1, Duration.ofMinutes(15))).isTrue();
        }
        assertThat(limiter.tryAcquire("key-10000", 1, Duration.ofMinutes(15))).isFalse();
        assertThat(limiter.tryAcquire("key-0", 1, Duration.ofMinutes(15))).isTrue();
    }

    @Test
    void concurrentAccessDoesNotExceedLimit() throws InterruptedException {
        BoundedExpiringPasswordResetRateLimiter limiter = new BoundedExpiringPasswordResetRateLimiter();
        int workers = 20;
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(workers);
        AtomicInteger accepted = new AtomicInteger();
        List<Thread> threads = new ArrayList<>();
        for (int index = 0; index < workers; index++) {
            Thread thread = new Thread(() -> {
                try {
                    start.await();
                    if (limiter.tryAcquire("concurrent", 5, Duration.ofMinutes(15))) {
                        accepted.incrementAndGet();
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
            threads.add(thread);
            thread.start();
        }
        start.countDown();
        done.await();
        assertThat(accepted).hasValue(5);
    }
}
