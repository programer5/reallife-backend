package com.example.backend.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Profile("!test")
@Component
@RequiredArgsConstructor
public class RedisLoginRateLimiter implements LoginRateLimiter {

    private final StringRedisTemplate redis;

    // 60초 동안 10회 (email+ip 기준)
    private static final int LIMIT = 10;
    private static final Duration WINDOW = Duration.ofSeconds(60);

    @Override
    public boolean allow(String email, String ip) {
        if (email == null || email.isBlank()) return false;

        String safeEmail = email.trim().toLowerCase();
        String safeIp = (ip == null || ip.isBlank()) ? "unknown" : ip.trim();

        String key = "rate:login:" + safeEmail + ":" + safeIp;

        Long count = redis.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redis.expire(key, WINDOW);
        }
        return count != null && count <= LIMIT;
    }
}