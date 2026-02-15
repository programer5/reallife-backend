package com.example.backend.sse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@Profile("!test")
@RequiredArgsConstructor
public class RedisSsePushService implements SsePushPort {

    private final RedisSsePubSub pubSub;

    @Override
    public void push(UUID userId, String eventName, Object payload, String eventId) {
        log.info("🟠 SSE publish | userId={} event={} eventId={}", userId, eventName, eventId);
        pubSub.publish(userId, eventName, eventId, payload);
    }
}