package com.example.backend.service.sse;

import com.example.backend.sse.SseEmitterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SseHeartbeatScheduler {

    private final SseEmitterRegistry registry;

    // 25초마다 ping (너무 짧게 하면 트래픽 증가, 너무 길면 중간에 끊길 수 있음)
    @Scheduled(fixedDelay = 25_000)
    public void heartbeat() {
        registry.broadcastPing();
    }
}