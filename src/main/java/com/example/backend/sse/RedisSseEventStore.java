package com.example.backend.sse;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Profile("!test")
@Component
@RequiredArgsConstructor
public class RedisSseEventStore implements SseEventStore {

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    // 유저당 보관 이벤트 수
    private static final int MAX_EVENTS_PER_USER = 200;

    // 키: sse:events:{userId}
    private static String key(UUID userId) {
        return "sse:events:" + userId;
    }

    /**
     * Redis 내부 저장용 DTO
     * - Lombok @Value 대신 record로 가면 IDE/Lombok 설정에 덜 의존해서 안전함
     */
    private record StoredEvent(
            String id,   // SSE event id (messageId/notificationId 등)
            String name, // event name
            String json, // payload json
            long ts      // 저장 시각
    ) {}
    @Override
    public void append(UUID userId, String eventName, String eventId, Object payload) {
        if (eventId == null || eventId.isBlank()) return;

        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            return;
        }

        StoredEvent se = new StoredEvent(eventId, eventName, payloadJson, System.currentTimeMillis());

        String serialized;
        try {
            serialized = objectMapper.writeValueAsString(se);
        } catch (Exception e) {
            return;
        }

        String k = key(userId);

        // ✅ 순서 유지: RPUSH(오래된 것 -> 새 것)
        redis.opsForList().rightPush(k, serialized);

        // ✅ 최근 MAX개만 유지 (왼쪽이 오래된 것)
        Long size = redis.opsForList().size(k);
        if (size != null && size > MAX_EVENTS_PER_USER) {
            long trimStart = Math.max(0, size - MAX_EVENTS_PER_USER);
            redis.opsForList().trim(k, trimStart, -1);
        }

        // ✅ TTL: 3일 보관
        redis.expire(k, 3, TimeUnit.DAYS);
    }

    /**
     * ✅ 인터페이스(SseEventStore) 시그니처에 맞춰서
     * StoredEvent -> SseStoredEvent 로 변환해서 반환
     */
    @Override
    public List<SseStoredEvent> replayAfter(UUID userId, String lastEventId) {
        if (lastEventId == null || lastEventId.isBlank()) return List.of();

        List<String> raw = redis.opsForList().range(key(userId), 0, -1);
        if (raw == null || raw.isEmpty()) return List.of();

        List<StoredEvent> events = new ArrayList<>(raw.size());
        for (String s : raw) {
            try {
                StoredEvent e = objectMapper.readValue(s, StoredEvent.class);
                events.add(e);
            } catch (Exception ignore) {
                // skip broken row
            }
        }

        int idx = -1;
        for (int i = 0; i < events.size(); i++) {
            if (lastEventId.equals(events.get(i).id())) {
                idx = i;
                break;
            }
        }
        if (idx < 0) return List.of();

        List<StoredEvent> missed = events.subList(idx + 1, events.size());

        // ✅ 외부로는 SseStoredEvent 타입으로 내보내기
        List<SseStoredEvent> result = new ArrayList<>(missed.size());
        for (StoredEvent e : missed) {
            result.add(new SseStoredEvent(e.id(), e.name(), e.json()));
        }
        return result;
    }

    @Override
    public Map<String, Object> payloadToMap(String payloadJson) {
        try {
            if (payloadJson == null || payloadJson.isBlank()) return Map.of();
            return objectMapper.readValue(payloadJson, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return Map.of("raw", payloadJson);
        }
    }
}