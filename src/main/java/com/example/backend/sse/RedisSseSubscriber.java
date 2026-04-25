package com.example.backend.sse;

import com.example.backend.monitoring.support.SseHealthTracker;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Profile("!test")
@Component
@RequiredArgsConstructor
public class RedisSseSubscriber {

    private final RedisMessageListenerContainer container;
    private final ObjectMapper objectMapper;
    private final SseEmitterRegistry registry;
    private final SseHealthTracker sseHealthTracker;

    @PostConstruct
    public void register() {
        container.addMessageListener(new MessageListener() {
            @Override
            public void onMessage(Message message, byte[] pattern) {
                try {
                    String body = new String(message.getBody());

                    RedisSsePubSub.PushMessage pm =
                            objectMapper.readValue(body, RedisSsePubSub.PushMessage.class);

                    UUID userId = UUID.fromString(pm.userId());
                    Map<String, Object> payload =
                            objectMapper.readValue(pm.payloadJson(), Map.class);

                    log.info("🔴 Redis pubsub received | userId={} event={} eventId={}",
                            userId, pm.eventName(), pm.eventId());

                    registry.send(userId, pm.eventName(), payload, pm.eventId());
                } catch (Exception e) {
                    sseHealthTracker.onFailure(e);
                    log.warn("Redis pubsub handle failed", e);
                }
            }
        }, new ChannelTopic(RedisSsePubSub.CHANNEL));

        log.info("🔴 Redis pubsub listener registered | channel={}", RedisSsePubSub.CHANNEL);
    }
}
