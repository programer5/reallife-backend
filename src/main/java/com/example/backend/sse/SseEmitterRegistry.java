package com.example.backend.sse;

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

    public SseEmitter register(UUID userId, long timeoutMillis) {
        SseEmitter emitter = new SseEmitter(timeoutMillis);

        emittersByUser.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        // 연결 종료/타임아웃/에러 시 emitter 정리
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

    /**
     * eventId 없이 전송 (기본)
     */
    public void send(UUID userId, String eventName, Object data) {
        send(userId, eventName, data, null);
    }

    /**
     * eventId 포함 전송 (Last-Event-ID replay 대응)
     */
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

            } catch (Exception ex) {
                // ✅ SSE는 클라이언트가 끊기는 것이 정상적인 케이스가 많음.
                // 예외를 밖으로 던지지 말고 emitter만 정리하고 끝낸다.
                remove(userId, emitter);
                try { emitter.complete(); } catch (Exception ignore) {}
            }
        }
    }

    /**
     * ✅ heartbeat/ping: 전체 연결에 ping 보내기
     * (프록시/로드밸런서에서 idle timeout 방지)
     */
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

        emitters.remove(emitter);
        if (emitters.isEmpty()) {
            emittersByUser.remove(userId);
        }

        log.info("🔴 SSE removed | userId={} remaining={}", userId,
                emittersByUser.getOrDefault(userId, List.of()).size());
    }
}