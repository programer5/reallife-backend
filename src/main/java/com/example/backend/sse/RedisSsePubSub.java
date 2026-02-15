package com.example.backend.sse;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RedisSsePubSub {

    public static final String CHANNEL = "sse:push";

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    // ✅ Jackson-friendly DTO
    public record PushMessage(
            String userId,
            String eventName,
            String eventId,
            String payloadJson
    ) {}

    public void publish(UUID userId, String eventName, String eventId, Object payload) {
        try {
            String payloadJson = objectMapper.writeValueAsString(payload);
            PushMessage msg = new PushMessage(userId.toString(), eventName, eventId, payloadJson);
            redis.convertAndSend(CHANNEL, objectMapper.writeValueAsString(msg));
        } catch (Exception e) {
            // 개발 중엔 로그 찍어두는 게 좋아
            // log.warn("Redis publish failed", e);
        }
    }
}