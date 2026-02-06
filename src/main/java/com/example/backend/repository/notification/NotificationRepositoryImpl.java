package com.example.backend.repository.notification;

import com.example.backend.domain.notification.Notification;
import com.example.backend.domain.notification.QNotification;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
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

    @Override
    public int softDeleteRead(UUID userId) {
        QNotification n = QNotification.notification;

        long updated = queryFactory
                .update(n)
                .set(n.deleted, true)
                .where(
                        n.userId.eq(userId),
                        n.deleted.isFalse(),
                        n.readAt.isNotNull()
                )
                .execute();

        return (int) updated;
    }

    @Override
    public List<Notification> findMyNotificationsByCursor(
            UUID userId,
            LocalDateTime cursorCreatedAt,
            UUID cursorId,
            int limit
    ) {
        QNotification n = QNotification.notification;

        return queryFactory
                .selectFrom(n)
                .where(
                        n.userId.eq(userId),
                        n.deleted.isFalse(),
                        cursorCondition(n, cursorCreatedAt, cursorId)
                )
                .orderBy(n.createdAt.desc(), n.id.desc())
                .limit(limit)
                .fetch();
    }

    private BooleanExpression cursorCondition(QNotification n, LocalDateTime cursorCreatedAt, UUID cursorId) {
        if (cursorCreatedAt == null || cursorId == null) return null;

        // (createdAt < cursorCreatedAt) OR (createdAt == cursorCreatedAt AND id < cursorId)
        return n.createdAt.lt(cursorCreatedAt)
                .or(n.createdAt.eq(cursorCreatedAt).and(n.id.lt(cursorId)));
    }
}