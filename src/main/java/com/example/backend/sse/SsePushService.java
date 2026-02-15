package com.example.backend.sse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SsePushService {

    private final RedisSsePubSub pubSub;

    public void push(UUID userId, String eventName, Object payload, String eventId) {
        log.info("🟠 SSE publish | userId={} event={} eventId={}", userId, eventName, eventId);
        pubSub.publish(userId, eventName, eventId, payload);
    }
}