package com.example.backend.service.auth;

import com.example.backend.domain.auth.RefreshToken;
import com.example.backend.domain.user.User;
import com.example.backend.exception.BusinessException;
import com.example.backend.exception.ErrorCode;
import com.example.backend.repository.auth.RefreshTokenRepository;
import com.example.backend.repository.user.UserRepository;
import com.example.backend.security.JwtProperties;
import com.example.backend.security.JwtTokenProvider;
import com.example.backend.security.RefreshTokenGenerator;
import com.example.backend.service.auth.dto.AuthTokens;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;

    public AuthTokens loginWithRefresh(String email, String rawPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));

        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        String access = jwtTokenProvider.createAccessToken(user.getId().toString(), user.getEmail());

        // refresh token 발급(opaque) + hash 저장
        String refreshRaw = RefreshTokenGenerator.newToken();
        String refreshHash = RefreshTokenGenerator.sha256Hex(refreshRaw);
        Instant exp = Instant.now().plus(jwtProperties.refreshTokenExpDays(), ChronoUnit.DAYS);

        refreshTokenRepository.save(RefreshToken.issue(user.getId(), refreshHash, exp));
        return new AuthTokens(access, refreshRaw);
    }

    /**
     * refresh-cookie로 들어온 refresh token으로 access 재발급 + refresh 로테이션(권장)
     */
    @Transactional
    public AuthTokens refreshRotate(String refreshRaw) {
        String hash = RefreshTokenGenerator.sha256Hex(refreshRaw);

        RefreshToken rt = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));

        if (rt.isExpired(Instant.now())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        // ✅ reuse detection: 이미 revoke된 refresh를 또 쓰려고 함 = 탈취 신호
        if (rt.isRevoked()) {
            // 해당 유저 전체 로그아웃(모든 refresh revoke)
            logoutAll(rt.getUserId());
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        User user = userRepository.findById(rt.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));

        // ✅ 로테이션: 기존 refresh 폐기 + 새 refresh 발급
        rt.revoke();

        String newRefreshRaw = RefreshTokenGenerator.newToken();
        String newRefreshHash = RefreshTokenGenerator.sha256Hex(newRefreshRaw);
        Instant exp = Instant.now().plus(jwtProperties.refreshTokenExpDays(), ChronoUnit.DAYS);
        refreshTokenRepository.save(RefreshToken.issue(user.getId(), newRefreshHash, exp));

        String newAccess = jwtTokenProvider.createAccessToken(user.getId().toString(), user.getEmail());
        return new AuthTokens(newAccess, newRefreshRaw);
    }

    public void revokeRefresh(String refreshRaw) {
        if (refreshRaw == null || refreshRaw.isBlank()) return;
        String hash = RefreshTokenGenerator.sha256Hex(refreshRaw);
        refreshTokenRepository.findByTokenHash(hash).ifPresent(rt -> {
            if (!rt.isRevoked()) rt.revoke();
        });
    }

    @Transactional
    public void logoutAll(UUID userId) {
        refreshTokenRepository.revokeAllByUserId(userId, Instant.now());
    }

    @Transactional
    public int cleanupRefreshTokens() {
        return refreshTokenRepository.deleteExpiredOrRevoked(Instant.now());
    }
}