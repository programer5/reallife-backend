package com.example.backend.sse;

import com.example.backend.monitoring.support.SseHealthTracker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Component
public class SseEmitterRegistry {

    private final Map<UUID, List<SseEmitter>> emittersByUser = new ConcurrentHashMap<>();
    private final SseHealthTracker sseHealthTracker;

    public SseEmitterRegistry(SseHealthTracker sseHealthTracker) {
        this.sseHealthTracker = sseHealthTracker;
    }

    public SseEmitter register(UUID userId, long timeoutMillis) {
        SseEmitter emitter = new SseEmitter(timeoutMillis);

        emittersByUser.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(emitter);
        sseHealthTracker.onConnected();

        emitter.onCompletion(() -> remove(userId, emitter));
        emitter.onTimeout(() -> {
            remove(userId, emitter);
            try { emitter.complete(); } catch (Exception ignore) {}
        });
        emitter.onError(e -> {
            remove(userId, emitter);
            try { emitter.complete(); } catch (Exception ignore) {}
        });

        log.info("🟢 SSE registered | userId={} connections={}", userId,
                emittersByUser.getOrDefault(userId, List.of()).size());

        return emitter;
    }

    public void send(UUID userId, String eventName, Object data) {
        send(userId, eventName, data, null);
    }

    public void send(UUID userId, String eventName, Object data, String eventId) {
        List<SseEmitter> emitters = emittersByUser.getOrDefault(userId, List.of());
        if (emitters.isEmpty()) return;

        for (SseEmitter emitter : emitters) {
            try {
                SseEmitter.SseEventBuilder event = SseEmitter.event().name(eventName);

                if (eventId != null && !eventId.isBlank()) {
                    event.id(eventId);
                }

                event.data(data);
                emitter.send(event);
                sseHealthTracker.onEventSent();

            } catch (Exception ex) {
                remove(userId, emitter);
                try { emitter.complete(); } catch (Exception ignore) {}
            }
        }
    }

    public void broadcastPing() {
        String id = String.valueOf(System.currentTimeMillis());
        Map<String, Object> payload = Map.of("ts", System.currentTimeMillis());

        for (UUID userId : emittersByUser.keySet()) {
            send(userId, "ping", payload, id);
        }
    }

    private void remove(UUID userId, SseEmitter emitter) {
        List<SseEmitter> emitters = emittersByUser.get(userId);
        if (emitters == null) return;

        boolean removed = emitters.remove(emitter);
        if (removed) {
            sseHealthTracker.onDisconnected();
        }

        if (emitters.isEmpty()) {
            emittersByUser.remove(userId);
        }

        log.info("🔴 SSE removed | userId={} remaining={}", userId,
                emittersByUser.getOrDefault(userId, List.of()).size());
    }
}