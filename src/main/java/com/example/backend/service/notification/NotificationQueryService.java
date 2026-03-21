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
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationQueryService {

    private final NotificationRepository notificationRepository;
    private final ConversationPinRepository conversationPinRepository;

    public NotificationCursorResponse getMyNotifications(UUID userId, String cursor, Integer size) {
        int pageSize = normalizeSize(size);
        Cursor parsedCursor = parseCursor(cursor);

        List<Notification> base = notificationRepository.findTop50ByUserIdAndDeletedFalseOrderByCreatedAtDesc(userId);
        Map<UUID, UUID> pinCidMap = conversationPinRepository.findAllById(
                base.stream()
                        .filter(n -> String.valueOf(n.getType()).startsWith("PIN_"))
                        .map(Notification::getRefId)
                        .filter(java.util.Objects::nonNull)
                        .distinct()
                        .toList()
        ).stream().collect(Collectors.toMap(p -> p.getId(), p -> p.getConversationId()));

        List<NotificationCursorResponse.Item> sorted = base.stream()
                .map(n -> toItem(n, pinCidMap))
                .sorted(Comparator
                        .comparingInt(NotificationCursorResponse.Item::priorityScore).reversed()
                        .thenComparing(NotificationCursorResponse.Item::createdAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        List<NotificationCursorResponse.Item> filtered = sorted.stream()
                .filter(item -> matchesCursor(item, parsedCursor))
                .limit(pageSize + 1L)
                .toList();

        boolean hasNext = filtered.size() > pageSize;
        List<NotificationCursorResponse.Item> page = hasNext ? filtered.subList(0, pageSize) : filtered;

        String nextCursor = null;
        if (hasNext && !page.isEmpty()) {
            NotificationCursorResponse.Item last = page.get(page.size() - 1);
            nextCursor = last.priorityScore() + "|" + last.createdAt();
        }

        boolean hasUnread = base.stream().anyMatch(n -> !n.isRead());
        return new NotificationCursorResponse(page, nextCursor, hasNext, hasUnread);
    }

    private boolean matchesCursor(NotificationCursorResponse.Item item, Cursor cursor) {
        if (cursor.priorityScore() == null || cursor.createdAt() == null) return true;
        if (item.priorityScore() < cursor.priorityScore()) return true;
        return item.priorityScore() == cursor.priorityScore()
                && item.createdAt() != null
                && item.createdAt().isBefore(cursor.createdAt());
    }

    private NotificationCursorResponse.Item toItem(Notification n, Map<UUID, UUID> pinCidMap) {
        UUID refId = n.getRefId();
        UUID ref2Id = n.getRef2Id();
        UUID conversationId = conversationIdOf(n, pinCidMap);
        String category = categoryOf(n.getType()).name();
        int priorityScore = priorityFor(n);
        String targetPath = targetPathOf(n, conversationId);

        return new NotificationCursorResponse.Item(
                n.getId(),
                n.getType().name(),
                refId,
                ref2Id,
                conversationId,
                category,
                priorityScore,
                targetPath,
                targetLabelOf(n.getType()),
                actionHintOf(n.getType()),
                n.getBody(),
                n.isRead(),
                n.getCreatedAt()
        );
    }

    private UUID conversationIdOf(Notification n, Map<UUID, UUID> pinCidMap) {
        if (String.valueOf(n.getType()).startsWith("PIN_")) {
            return n.getRefId() == null ? null : pinCidMap.get(n.getRefId());
        }
        if (n.getType() == NotificationType.MESSAGE_RECEIVED) {
            return n.getRefId();
        }
        return null;
    }

    private String targetPathOf(Notification n, UUID conversationId) {
        String notiId = enc(n.getId());
        if (n.getType() == NotificationType.MESSAGE_RECEIVED) {
            if (conversationId == null) return "/inbox/conversations";
            if (n.getRef2Id() != null) {
                return "/inbox/conversations/" + enc(conversationId) + "?fromNoti=1&notiId=" + notiId + "&mid=" + enc(n.getRef2Id());
            }
            return "/inbox/conversations/" + enc(conversationId) + "?fromNoti=1&notiId=" + notiId;
        }
        if (String.valueOf(n.getType()).startsWith("PIN_")) {
            if (conversationId == null) return "/inbox/conversations";
            if (n.getRef2Id() != null) {
                return "/inbox/conversations/" + enc(conversationId) + "?fromNoti=1&notiId=" + notiId + "&mid=" + enc(n.getRef2Id()) + "&pinId=" + enc(n.getRefId());
            }
            return "/inbox/conversations/" + enc(conversationId) + "/pins?notiId=" + notiId + "&pinId=" + enc(n.getRefId());
        }
        if (n.getType() == NotificationType.POST_COMMENT || n.getType() == NotificationType.POST_LIKE) {
            UUID postId = n.getRef2Id() != null ? n.getRef2Id() : n.getRefId();
            return postId != null ? "/posts/" + enc(postId) + "?fromNoti=1&notiId=" + notiId : "/";
        }
        if (n.getType() == NotificationType.FOLLOW) {
            return "/me";
        }
        return "/inbox";
    }

    private String targetLabelOf(NotificationType type) {
        return switch (type) {
            case PIN_REMIND, PIN_CREATED, PIN_UPDATED, PIN_DONE, PIN_CANCELED, PIN_DISMISSED -> "원본 대화 열기";
            case MESSAGE_RECEIVED -> "대화로 이동";
            case POST_COMMENT, POST_LIKE -> "게시글 보기";
            case FOLLOW -> "프로필 흐름 보기";
        };
    }

    private String actionHintOf(NotificationType type) {
        return switch (type) {
            case PIN_REMIND -> "원본 메시지 위치로 바로 이동해서 약속·할일 흐름 이어가기";
            case MESSAGE_RECEIVED -> "대화로 이동해서 답장하거나 액션 만들기";
            case POST_COMMENT -> "댓글 맥락을 보고 다음 행동을 정리하기";
            case POST_LIKE -> "게시글 반응을 확인하고 이어서 공유 흐름 열기";
            case FOLLOW -> "새 관계 흐름을 확인하고 필요하면 인사 보내기";
            case PIN_CREATED, PIN_UPDATED, PIN_DONE, PIN_CANCELED, PIN_DISMISSED -> "액션 상태를 확인하고 필요한 후속 작업 정리하기";
        };
    }

    private Category categoryOf(NotificationType type) {
        return switch (type) {
            case PIN_REMIND -> Category.REMINDER;
            case MESSAGE_RECEIVED -> Category.MESSAGE;
            case POST_COMMENT -> Category.COMMENT;
            case POST_LIKE, FOLLOW -> Category.REACTION;
            case PIN_CREATED, PIN_UPDATED, PIN_DONE, PIN_CANCELED, PIN_DISMISSED -> Category.ACTION;
        };
    }

    private int priorityFor(Notification n) {
        int unreadBoost = n.isRead() ? 0 : 1000;
        return unreadBoost + switch (n.getType()) {
            case PIN_REMIND -> 500;
            case MESSAGE_RECEIVED -> 400;
            case POST_COMMENT -> 300;
            case PIN_CREATED, PIN_UPDATED, PIN_DONE, PIN_CANCELED, PIN_DISMISSED -> 250;
            case POST_LIKE, FOLLOW -> 150;
        };
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
        if (parts.length == 2) {
            try {
                return new Cursor(Integer.parseInt(parts[0]), LocalDateTime.parse(parts[1]));
            } catch (Exception ignored) {
            }
        }
        return new Cursor(null, null);
    }

    private String enc(UUID value) {
        return value == null ? "" : value.toString();
    }

    private enum Category {REMINDER, MESSAGE, COMMENT, REACTION, ACTION}
    private record Cursor(Integer priorityScore, LocalDateTime createdAt) {}
}
