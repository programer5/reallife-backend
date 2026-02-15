package com.example.backend.sse;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@Profile("test")
@RequiredArgsConstructor
public class NoopSseEventStore implements SseEventStore {

    private final ObjectMapper objectMapper;

    @Override
    public List<SseStoredEvent> replayAfter(UUID userId, String lastEventId) {
        return Collections.emptyList(); // ✅ test에서는 replay 없음
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> payloadToMap(String json) {
        try {
            if (json == null || json.isBlank()) return Map.of();
            return objectMapper.readValue(json, Map.class);
        } catch (Exception e) {
            return Map.of();
        }
    }
}