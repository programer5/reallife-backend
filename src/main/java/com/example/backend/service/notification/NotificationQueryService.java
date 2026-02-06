package com.example.backend.service.notification;

import com.example.backend.controller.notification.dto.NotificationCursorResponse;
import com.example.backend.domain.notification.Notification;
import com.example.backend.repository.notification.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationQueryService {

    private final NotificationRepository notificationRepository;

    @Transactional(readOnly = true)
    public NotificationCursorResponse getMyNotifications(UUID meId, String cursor, Integer size) {
        int pageSize = normalizeSize(size);
        Cursor parsed = parseCursor(cursor);

        List<Notification> fetched = notificationRepository.findMyNotificationsByCursor(
                meId,
                parsed.createdAt,
                parsed.id,
                pageSize + 1
        );

        boolean hasNext = fetched.size() > pageSize;
        List<Notification> page = hasNext ? fetched.subList(0, pageSize) : fetched;

        boolean hasUnread = notificationRepository.existsByUserIdAndReadAtIsNullAndDeletedFalse(meId);

        return NotificationCursorResponse.of(page, hasNext, hasUnread);
    }

    private int normalizeSize(Integer size) {
        int v = (size == null) ? 20 : size;
        if (v < 1) v = 1;
        if (v > 50) v = 50;
        return v;
    }

    private Cursor parseCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) return new Cursor(null, null);

        String[] parts = cursor.split("\\|");
        if (parts.length != 2) return new Cursor(null, null);

        try {
            LocalDateTime createdAt = LocalDateTime.parse(parts[0]);
            UUID id = UUID.fromString(parts[1]);
            return new Cursor(createdAt, id);
        } catch (Exception e) {
            // 커서가 이상하면 첫 페이지로 처리
            return new Cursor(null, null);
        }
    }

    private record Cursor(LocalDateTime createdAt, UUID id) {}
}