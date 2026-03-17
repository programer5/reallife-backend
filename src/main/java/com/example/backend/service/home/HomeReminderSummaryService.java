package com.example.backend.service.home;

import com.example.backend.controller.home.dto.HomeReminderSummaryResponse;
import com.example.backend.domain.notification.Notification;
import com.example.backend.domain.notification.NotificationType;
import com.example.backend.repository.notification.NotificationRepository;
import com.example.backend.repository.pin.ConversationPinRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class HomeReminderSummaryService {

    private final EntityManager em;
    private final NotificationRepository notificationRepository;
    private final ConversationPinRepository pinRepository;

    @Transactional(readOnly = true)
    public HomeReminderSummaryResponse getSummary(UUID userId, Boolean browserNotifyEnabled) {
        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = start.plusDays(1);

        long unreadCount = em.createQuery(
                        """
                        select count(n)
                          from Notification n
                         where n.deleted = false
                           and n.userId = :userId
                           and n.readAt is null
                        """,
                        Long.class
                )
                .setParameter("userId", userId)
                .getSingleResult();

        long unreadReminderCount = em.createQuery(
                        """
                        select count(n)
                          from Notification n
                         where n.deleted = false
                           and n.userId = :userId
                           and n.readAt is null
                           and n.type = :type
                        """,
                        Long.class
                )
                .setParameter("userId", userId)
                .setParameter("type", NotificationType.PIN_REMIND)
                .getSingleResult();

        long todayReminderCount = em.createQuery(
                        """
                        select count(n)
                          from Notification n
                         where n.deleted = false
                           and n.userId = :userId
                           and n.type = :type
                           and n.createdAt >= :start
                           and n.createdAt < :end
                        """,
                        Long.class
                )
                .setParameter("userId", userId)
                .setParameter("type", NotificationType.PIN_REMIND)
                .setParameter("start", start)
                .setParameter("end", end)
                .getSingleResult();

        List<Notification> top = notificationRepository.findTop50ByUserIdAndDeletedFalseOrderByCreatedAtDesc(userId);
        Notification lead = top.stream()
                .filter(n -> !n.isRead() && n.getType() == NotificationType.PIN_REMIND)
                .findFirst()
                .orElseGet(() -> top.stream().filter(n -> !n.isRead()).findFirst().orElse(top.isEmpty() ? null : top.get(0)));

        HomeReminderSummaryResponse.Lead leadDto = null;
        if (lead != null) {
            UUID conversationId = resolveConversationId(lead);
            leadDto = new HomeReminderSummaryResponse.Lead(
                    lead.getId(),
                    lead.getType().name(),
                    lead.getRefId(),
                    lead.getRef2Id(),
                    conversationId,
                    lead.getBody(),
                    lead.isRead(),
                    lead.getCreatedAt()
            );
        }

        return new HomeReminderSummaryResponse(
                new HomeReminderSummaryResponse.Summary(unreadCount, unreadReminderCount, todayReminderCount),
                new HomeReminderSummaryResponse.Settings(Boolean.TRUE.equals(browserNotifyEnabled), "CLIENT_SYNC"),
                leadDto
        );
    }

    private UUID resolveConversationId(Notification notification) {
        if (notification == null) return null;
        if (notification.getType() == NotificationType.MESSAGE_RECEIVED) {
            return notification.getRefId();
        }
        if (notification.getType() == NotificationType.PIN_CREATED
                || notification.getType() == NotificationType.PIN_REMIND
                || notification.getType() == NotificationType.PIN_DISMISSED
                || notification.getType() == NotificationType.PIN_CANCELED
                || notification.getType() == NotificationType.PIN_DONE
                || notification.getType() == NotificationType.PIN_UPDATED) {
            if (notification.getRefId() == null) return null;
            List<ConversationPinRepository.PinConversationRow> rows = pinRepository.findConversationIdsByPinIds(List.of(notification.getRefId()));
            return rows.isEmpty() ? null : rows.get(0).getConversationId();
        }
        return null;
    }
}

