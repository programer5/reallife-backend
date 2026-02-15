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
    private final SseEventStore eventStore;

    @Override
    public void push(UUID userId, String eventName, Object payload, String eventId) {
        // ✅ 1) id 규칙 통일 (msg:/noti:)
        String formattedId = SseEventIds.format(eventName, eventId);

        // ✅ 2) replay(Last-Event-ID)용 저장
        eventStore.append(userId, eventName, formattedId, payload);

        // ✅ 3) 실시간 전송 (멀티 인스턴스 fan-out)
        log.info("🟠 SSE publish | userId={} event={} eventId={}", userId, eventName, formattedId);
        pubSub.publish(userId, eventName, formattedId, payload);
    }
}