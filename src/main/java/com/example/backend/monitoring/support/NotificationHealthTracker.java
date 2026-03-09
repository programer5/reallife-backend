package com.example.backend.monitoring.support;

import com.example.backend.domain.notification.NotificationType;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class NotificationHealthTracker {

    private final AtomicReference<LocalDateTime> lastCreatedAt = new AtomicReference<>(null);
    private final Map<NotificationType, AtomicReference<LocalDateTime>> byType =
            new EnumMap<>(NotificationType.class);

    public NotificationHealthTracker() {
        for (NotificationType type : NotificationType.values()) {
            byType.put(type, new AtomicReference<>(null));
        }
    }

    public void markCreated(NotificationType type) {
        LocalDateTime now = LocalDateTime.now();
        lastCreatedAt.set(now);

        AtomicReference<LocalDateTime> ref = byType.get(type);
        if (ref != null) {
            ref.set(now);
        }
    }

    public LocalDateTime getLastCreatedAt() {
        return lastCreatedAt.get();
    }

    public LocalDateTime getLastCreatedAt(NotificationType type) {
        AtomicReference<LocalDateTime> ref = byType.get(type);
        return ref == null ? null : ref.get();
    }
}