package com.example.backend.security;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LoginRateLimiter {

    private static final int MAX_ATTEMPTS = 10;      // 10회
    private static final long WINDOW_SEC = 60;       // 60초

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public boolean allow(String key) {
        long now = Instant.now().getEpochSecond();

        Bucket b = buckets.computeIfAbsent(key, k -> new Bucket(now, 0));
        synchronized (b) {
            if (now - b.windowStart >= WINDOW_SEC) {
                b.windowStart = now;
                b.attempts = 0;
            }
            b.attempts++;
            return b.attempts <= MAX_ATTEMPTS;
        }
    }

    private static class Bucket {
        volatile long windowStart;
        volatile int attempts;
        Bucket(long windowStart, int attempts) {
            this.windowStart = windowStart;
            this.attempts = attempts;
        }
    }
}
