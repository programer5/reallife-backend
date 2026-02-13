package com.example.backend.sse;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class SseEventStore {

    private final ObjectMapper objectMapper;

    // 유저별 최근 이벤트 저장(메모리)
    private final Map<UUID, Deque<StoredEvent>> store = new ConcurrentHashMap<>();

    // 유저당 최대 보관 개수(필요하면 조절)
    private static final int MAX_EVENTS_PER_USER = 200;

    @Value
    public static class StoredEvent {
        String id;       // SSE event id (messageId/notificationId 등)
        String name;     // event name (message-created / notification-created)
        String json;     // data payload JSON
        long ts;         // 저장 시각
    }

    public void append(UUID userId, String eventName, String eventId, Object payload) {
        if (eventId == null || eventId.isBlank()) return; // replay하려면 id가 있어야 함

        String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize SSE payload. userId={}, event={}", userId, eventName, e);
            return;
        }

        Deque<StoredEvent> q = store.computeIfAbsent(userId, k -> new ArrayDeque<>());
        synchronized (q) {
            q.addLast(new StoredEvent(eventId, eventName, json, System.currentTimeMillis()));
            while (q.size() > MAX_EVENTS_PER_USER) {
                q.removeFirst();
            }
        }
    }

    /**
     * lastEventId "이후"의 이벤트들을 순서대로 반환
     * - lastEventId가 null이면 빈 리스트 반환(=replay 없음)
     * - lastEventId를 찾지 못하면(너무 오래 전/버퍼에서 밀림) 빈 리스트 반환(단순화)
     */
    public List<StoredEvent> replayAfter(UUID userId, String lastEventId) {
        if (lastEventId == null || lastEventId.isBlank()) return List.of();

        Deque<StoredEvent> q = store.get(userId);
        if (q == null) return List.of();

        List<StoredEvent> result = new ArrayList<>();
        synchronized (q) {
            boolean found = false;
            for (StoredEvent e : q) {
                if (!found) {
                    if (e.id.equals(lastEventId)) {
                        found = true;
                    }
                    continue;
                }
                result.add(e);
            }
        }
        return result;
    }
}