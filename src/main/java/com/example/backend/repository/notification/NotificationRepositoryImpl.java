package com.example.backend.repository.notification;

import com.example.backend.domain.notification.Notification;
import com.example.backend.domain.notification.NotificationType;
import com.example.backend.domain.notification.QNotification;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.NumberExpression;
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
                .where(n.userId.eq(userId), n.deleted.isFalse(), n.readAt.isNull())
                .execute();
        return (int) updated;
    }

    @Override
    public int markAsReadIfUnread(UUID notificationId, UUID userId, LocalDateTime now) {
        QNotification n = QNotification.notification;
        long updated = queryFactory
                .update(n)
                .set(n.readAt, now)
                .where(n.id.eq(notificationId), n.userId.eq(userId), n.deleted.isFalse(), n.readAt.isNull())
                .execute();
        return (int) updated;
    }

    @Override
    public int hardDeleteRead(UUID userId) {
        QNotification n = QNotification.notification;
        long deleted = queryFactory.delete(n).where(n.userId.eq(userId), n.readAt.isNotNull()).execute();
        return (int) deleted;
    }

    @Override
    public List<Notification> findMyNotificationsByCursor(UUID userId, Integer cursorPriorityScore, LocalDateTime cursorCreatedAt, int limit) {
        QNotification n = QNotification.notification;
        NumberExpression<Integer> priority = priorityExpr(n);
        return queryFactory
                .selectFrom(n)
                .where(
                        n.userId.eq(userId),
                        n.deleted.isFalse(),
                        cursorCondition(priority, n, cursorPriorityScore, cursorCreatedAt)
                )
                .orderBy(priority.desc(), n.createdAt.desc())
                .limit(limit)
                .fetch();
    }

    private BooleanExpression cursorCondition(NumberExpression<Integer> priority, QNotification n, Integer cursorPriorityScore, LocalDateTime cursorCreatedAt) {
        if (cursorPriorityScore == null || cursorCreatedAt == null) return null;
        return priority.lt(cursorPriorityScore)
                .or(priority.eq(cursorPriorityScore).and(n.createdAt.lt(cursorCreatedAt)));
    }

    private NumberExpression<Integer> priorityExpr(QNotification n) {
        return n.readAt.isNull().when(true).then(1000).otherwise(0)
                .add(new com.querydsl.core.types.dsl.CaseBuilder()
                        .when(n.type.eq(NotificationType.PIN_REMIND)).then(500)
                        .when(n.type.eq(NotificationType.MESSAGE_RECEIVED)).then(400)
                        .when(n.type.eq(NotificationType.POST_COMMENT)).then(300)
                        .when(n.type.in(
                                NotificationType.PIN_CREATED,
                                NotificationType.PIN_UPDATED,
                                NotificationType.PIN_DONE,
                                NotificationType.PIN_CANCELED,
                                NotificationType.PIN_DISMISSED
                        )).then(250)
                        .when(n.type.in(NotificationType.POST_LIKE, NotificationType.FOLLOW)).then(150)
                        .otherwise(100));
    }
}
