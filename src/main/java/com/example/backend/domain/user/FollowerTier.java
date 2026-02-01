package com.example.backend.domain.user;

public enum FollowerTier {
    NONE,
    SILVER,
    GOLD,
    PLATINUM;

    public static FollowerTier of(long followerCount) {
        if (followerCount >= 1_000_000) return PLATINUM;
        if (followerCount >= 500_000) return GOLD;
        if (followerCount >= 100_000) return SILVER;
        return NONE;
    }
}
