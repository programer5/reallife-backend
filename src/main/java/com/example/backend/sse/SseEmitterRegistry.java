package com.example.backend.sse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Component
public class SseEmitterRegistry {

    private final Map<UUID, List<SseEmitter>> emittersByUser = new ConcurrentHashMap<>();

    public SseEmitter register(UUID userId, long timeoutMillis) {
        SseEmitter emitter = new SseEmitter(timeoutMillis);

        emittersByUser.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> remove(userId, emitter));
        emitter.onTimeout(() -> remove(userId, emitter));
        emitter.onError(e -> remove(userId, emitter));

        log.info("🟢 SSE registered | userId={} connections={}", userId, emittersByUser.get(userId).size());
        return emitter;
    }

    public void send(UUID userId, String eventName, Object data, String eventId) {
        List<SseEmitter> emitters = emittersByUser.getOrDefault(userId, List.of());
        if (emitters.isEmpty()) return;

        for (SseEmitter emitter : emitters) {
            try {
                SseEmitter.SseEventBuilder event = SseEmitter.event().name(eventName).data(data);
                if (eventId != null) event.id(eventId);
                emitter.send(event);
            } catch (IOException | IllegalStateException e) {
                remove(userId, emitter);
            }
        }
    }

    /** ✅ heartbeat용: 전체 연결에 ping 보내기 */
    public void broadcastPing() {
        for (UUID userId : emittersByUser.keySet()) {
            send(userId, "ping", Map.of("ts", System.currentTimeMillis()), null);
        }
    }

    private void remove(UUID userId, SseEmitter emitter) {
        List<SseEmitter> emitters = emittersByUser.get(userId);
        if (emitters == null) return;

        emitters.remove(emitter);
        if (emitters.isEmpty()) emittersByUser.remove(userId);

        log.info("🔴 SSE removed | userId={} remaining={}", userId, emittersByUser.getOrDefault(userId, List.of()).size());
    }
}