package com.example.backend.controller.sse;

import com.example.backend.sse.SseEmitterRegistry;
import com.example.backend.sse.SseEventStore;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/sse")
public class SseController {

    private final SseEmitterRegistry registry;
    private final SseEventStore eventStore;

    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(
            @AuthenticationPrincipal String userId,
            @RequestHeader(value = "Last-Event-ID", required = false) String lastEventId,
            HttpServletResponse response
    ) {
        try {
            if (userId == null || userId.isBlank()) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증이 필요합니다.");
            }

            UUID meId = UUID.fromString(userId);

            response.setHeader("X-Accel-Buffering", "no");
            response.setHeader(HttpHeaders.CACHE_CONTROL, "no-cache");

            long timeout = 30L * 60 * 1000;
            SseEmitter emitter = registry.register(meId, timeout);

            // 연결 확인 이벤트
            Map<String, Object> payload = new HashMap<>();
            payload.put("serverTime", LocalDateTime.now().toString());
            if (lastEventId != null) {
                payload.put("lastEventId", lastEventId);
            }
            registry.send(meId, "connected", payload, null);

            var missed = eventStore.replayAfter(meId, lastEventId);
            for (var e : missed) {
                registry.send(
                        meId,
                        e.name(),
                        eventStore.payloadToMap(e.json()),
                        e.id()
                );
            }

            return emitter;
        } catch (Exception e) {
            // 이 로그가 찍히면 "컨트롤러 내부에서 터진 것" 확정
            log.error("SSE subscribe failed. userId={}", userId, e);
            throw e;
        }
    }
}