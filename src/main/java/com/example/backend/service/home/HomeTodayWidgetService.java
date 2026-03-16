package com.example.backend.service.home;

import com.example.backend.controller.home.dto.HomeTodayWidgetResponse;
import com.example.backend.domain.pin.ConversationPin;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class HomeTodayWidgetService {

    private final EntityManager em;

    public HomeTodayWidgetResponse getTodayWidget(UUID userId) {
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = start.plusDays(1);
        LocalDateTime now = LocalDateTime.now();

        List<ConversationPin> pins = em.createQuery(
                        """
                        select p
                          from ConversationPin p
                         where p.deleted = false
                           and p.createdBy = :userId
                           and p.startAt is not null
                           and p.startAt >= :start
                           and p.startAt < :end
                         order by p.startAt asc, p.createdAt desc
                        """,
                        ConversationPin.class
                )
                .setParameter("userId", userId)
                .setParameter("start", start)
                .setParameter("end", end)
                .setMaxResults(12)
                .getResultList();

        List<HomeTodayWidgetResponse.Item> items = pins.stream()
                .map(this::toItem)
                .toList();

        int total = items.size();
        int done = (int) items.stream().filter(item -> "DONE".equals(item.status())).count();
        int upcoming = (int) items.stream().filter(item -> item.startAt() != null
                        && !"DONE".equals(item.status())
                        && !"CANCELED".equals(item.status())
                        && !item.startAt().isBefore(now))
                .count();

        return new HomeTodayWidgetResponse(
                new HomeTodayWidgetResponse.Summary(total, upcoming, done),
                items
        );
    }

    private HomeTodayWidgetResponse.Item toItem(ConversationPin pin) {
        return new HomeTodayWidgetResponse.Item(
                pin.getId(),
                pin.getConversationId(),
                pin.getSourceMessageId(),
                pin.getType().name(),
                pin.getTitle(),
                pin.getPlaceText(),
                pin.getStartAt(),
                pin.getRemindAt(),
                pin.getStatus().name()
        );
    }
}
