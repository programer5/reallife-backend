package com.example.backend.sse;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface SseEventStore {

    /**
     * Last-Event-ID 재전송(replay)을 위해 유저별 이벤트를 저장한다.
     * eventId는 SSE의 id 값(= prefix 적용된 값)으로 저장하는 것을 권장.
     */
    void append(UUID userId, String eventName, String eventId, Object payload);

    /**
     * lastEventId 이후의 이벤트들을 반환한다.
     * lastEventId는 클라이언트가 보낸 Last-Event-ID 헤더 값.
     */
    List<SseStoredEvent> replayAfter(UUID userId, String lastEventId);

    /**
     * 저장된 JSON payload를 Map으로 변환한다.
     */
    Map<String, Object> payloadToMap(String json);
}