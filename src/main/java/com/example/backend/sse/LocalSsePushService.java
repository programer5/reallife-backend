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

    @Override
    public void push(UUID userId, String eventName, Object payload, String eventId) {
        registry.send(userId, eventName, payload, eventId);
    }
}