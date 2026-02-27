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
                        cursorConditionByCreatedAtOnly(n, cursorCreatedAt)
                )
                // ✅ 정렬은 기존처럼 유지 (id는 tie-breaker용)
                .orderBy(
                        n.createdAt.desc(),
                        n.id.desc()
                )
                .limit(limit)
                .fetch();
    }

    /**
     * ✅ FIX: H2/UUID 비교 이슈 회피
     * - cursor는 createdAt만 사용
     * - id 기반 lt 비교 제거
     */
    private BooleanExpression cursorConditionByCreatedAtOnly(QNotification n, LocalDateTime cursorCreatedAt) {
        if (cursorCreatedAt == null) return null;
        return n.createdAt.lt(cursorCreatedAt);
    }
}