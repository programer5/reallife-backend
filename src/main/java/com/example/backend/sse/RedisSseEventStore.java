package com.example.backend.sse;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class RedisSseEventStore {

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    // 유저당 보관 이벤트 수
    private static final int MAX_EVENTS_PER_USER = 200;

    // 키: sse:events:{userId}
    private static String key(UUID userId) {
        return "sse:events:" + userId;
    }

    @Value
    public static class StoredEvent {
        String id;       // SSE event id (messageId/notificationId 등)
        String name;     // event name
        String json;     // payload json
        long ts;         // 저장 시각
    }

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

        // ✅ TTL(선택): 3일 보관
        redis.expire(k, 3, TimeUnit.DAYS);
    }

    public List<StoredEvent> replayAfter(UUID userId, String lastEventId) {
        if (lastEventId == null || lastEventId.isBlank()) return List.of();

        List<String> raw = redis.opsForList().range(key(userId), 0, -1);
        if (raw == null || raw.isEmpty()) return List.of();

        List<StoredEvent> events = new ArrayList<>(raw.size());
        for (String s : raw) {
            try {
                StoredEvent e = objectMapper.readValue(s, StoredEvent.class);
                events.add(e);
            } catch (Exception ignore) { }
        }

        int idx = -1;
        for (int i = 0; i < events.size(); i++) {
            if (lastEventId.equals(events.get(i).getId())) {
                idx = i;
                break;
            }
        }
        if (idx < 0) return List.of();

        return events.subList(idx + 1, events.size());
    }

    public Map<String, Object> payloadToMap(String payloadJson) {
        try {
            return objectMapper.readValue(payloadJson, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return Map.of("raw", payloadJson);
        }
    }
}