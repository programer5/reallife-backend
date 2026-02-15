package com.example.backend.sse;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Profile("test")
@RequiredArgsConstructor
public class LocalSsePushService implements SsePushPort {

    private final SseEmitterRegistry registry;
    private final SseEventStore eventStore;

    @Override
    public void push(UUID userId, String eventName, Object payload, String eventId) {
        String formattedId = SseEventIds.format(eventName, eventId);

        // test 프로필에서는 보통 Noop이라 실제 저장은 안 하지만,
        // 인터페이스 규칙은 동일하게 맞춰두는 게 유지보수에 좋음
        eventStore.append(userId, eventName, formattedId, payload);
        registry.send(userId, eventName, payload, formattedId);
    }
}