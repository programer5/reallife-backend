package com.example.backend.auth;

import com.example.backend.service.auth.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RefreshTokenCleanupJob {

    private final AuthService authService;

    // 매일 새벽 4시
    @Scheduled(cron = "0 0 4 * * *")
    public void cleanup() {
        int deleted = authService.cleanupRefreshTokens();
        log.info("RefreshToken cleanup deleted={}", deleted);
    }
}