package com.example.backend.repository.auth;

import com.example.backend.domain.auth.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("""
        update RefreshToken rt
           set rt.revokedAt = :now
         where rt.userId = :userId
           and rt.revokedAt is null
    """)
    int revokeAllByUserId(UUID userId, Instant now);

    @Modifying
    @Query("""
        delete from RefreshToken rt
         where rt.expiresAt < :now
            or rt.revokedAt is not null
    """)
    int deleteExpiredOrRevoked(Instant now);
}