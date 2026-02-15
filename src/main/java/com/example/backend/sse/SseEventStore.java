package com.example.backend.sse;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface SseEventStore {
    List<SseStoredEvent> replayAfter(UUID userId, String lastEventId);
    Map<String, Object> payloadToMap(String json);
}