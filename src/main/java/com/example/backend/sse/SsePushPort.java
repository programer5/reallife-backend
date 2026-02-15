package com.example.backend.sse;

import java.util.UUID;

public interface SsePushPort {
    void push(UUID userId, String eventName, Object payload, String eventId);
}