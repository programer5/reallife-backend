package com.example.backend.security;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Profile("test")
@Component
public class TestLoginRateLimiter implements LoginRateLimiter {
    @Override
    public boolean allow(String email, String ip) {
        return true;
    }
}