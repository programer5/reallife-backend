package com.example.backend.service.notification;

import com.example.backend.controller.notification.dto.NotificationCursorResponse;
import com.example.backend.domain.notification.Notification;
import com.example.backend.domain.notification.NotificationType;
import com.example.backend.repository.notification.NotificationRepository;
import com.example.backend.repository.pin.ConversationPinRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationQueryService {

    private final NotificationRepository notificationRepository;
    private final ConversationPinRepository pinRepository;

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

        // ✅ PIN 알림(refId=pinId)들만 pinId -> conversationId 매핑
        List<UUID> pinIds = page.stream()
                .filter(n -> n.getType() == NotificationType.PIN_CREATED || n.getType() == NotificationType.PIN_REMIND)
                .map(Notification::getRefId)
                .filter(id -> id != null)
                .distinct()
                .toList();

        Map<UUID, UUID> pinCidMap = pinIds.isEmpty()
                ? Collections.emptyMap()
                : pinRepository.findConversationIdsByPinIds(pinIds).stream()
                .collect(Collectors.toMap(
                        ConversationPinRepository.PinConversationRow::getId,
                        ConversationPinRepository.PinConversationRow::getConversationId
                ));

        // ✅ conversationId 포함해서 items 구성
        List<NotificationCursorResponse.Item> items = page.stream()
                .map(n -> new NotificationCursorResponse.Item(
                        n.getId(),
                        n.getType().name(),
                        n.getRefId(),
                        pinCidMap.get(n.getRefId()), // ✅ PIN_*면 값, 아니면 null
                        n.getBody(),
                        n.isRead(),
                        n.getCreatedAt()
                ))
                .toList();

        String nextCursor = null;
        if (hasNext && !page.isEmpty()) {
            Notification last = page.get(page.size() - 1);
            nextCursor = last.getCreatedAt().toString() + "|" + last.getId();
        }

        return new NotificationCursorResponse(items, nextCursor, hasNext, hasUnread);
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