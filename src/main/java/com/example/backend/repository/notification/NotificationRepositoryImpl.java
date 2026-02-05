package com.example.backend.repository.notification;

import com.example.backend.domain.notification.QNotification;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@RequiredArgsConstructor
public class NotificationRepositoryImpl implements NotificationRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public int markAllAsRead(UUID userId, LocalDateTime now) {
        QNotification n = QNotification.notification;

        long updated = queryFactory
                .update(n)
                .set(n.readAt, now)
                .where(
                        n.userId.eq(userId),
                        n.deleted.isFalse(),
                        n.readAt.isNull()
                )
                .execute();

        return (int) updated;
    }

    @Override
    public int markAsReadIfUnread(UUID notificationId, UUID userId, LocalDateTime now) {
        QNotification n = QNotification.notification;

        long updated = queryFactory
                .update(n)
                .set(n.readAt, now)
                .where(
                        n.id.eq(notificationId),
                        n.userId.eq(userId),
                        n.deleted.isFalse(),
                        n.readAt.isNull()
                )
                .execute();

        return (int) updated;
    }
}