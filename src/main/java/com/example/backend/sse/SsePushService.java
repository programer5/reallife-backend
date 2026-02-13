package com.example.backend.sse;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SsePushService {

    private final SseEmitterRegistry registry;
    private final SseEventStore eventStore;

    public void push(UUID userId, String eventName, Object payload, String eventId) {
        // 1) 먼저 저장(재연결 replay용)
        eventStore.append(userId, eventName, eventId, payload);

        // 2) 실시간 전송
        registry.send(userId, eventName, payload, eventId);
    }
}