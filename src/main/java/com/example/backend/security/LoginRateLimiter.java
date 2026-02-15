package com.example.backend.security;

public interface LoginRateLimiter {
    boolean allow(String email, String ip);
}