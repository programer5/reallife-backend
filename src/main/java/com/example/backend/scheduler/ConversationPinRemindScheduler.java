package com.example.backend.scheduler;

import com.example.backend.domain.notification.NotificationType;
import com.example.backend.domain.pin.ConversationPin;
import com.example.backend.repository.message.ConversationMemberRepository;
import com.example.backend.repository.pin.ConversationPinRepository;
import com.example.backend.service.notification.NotificationCommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ConversationPinRemindScheduler {

    private final ConversationPinRepository pinRepository;
    private final ConversationMemberRepository memberRepository;
    private final NotificationCommandService notificationCommandService;

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final int BATCH_SIZE = 200;

    // ✅ 매분 실행(중복은 NotificationCommandService.createIfNotExists로 방지)
    @Scheduled(fixedDelay = 60_000)
    public void run() {
        LocalDateTime now = LocalDateTime.now();

        List<ConversationPin> duePins = pinRepository.findDueReminds(now, PageRequest.of(0, BATCH_SIZE));
        if (duePins.isEmpty()) return;

        int sent = 0;

        for (ConversationPin pin : duePins) {

            int claimed = pinRepository.claimRemind(pin.getId(), now);
            if (claimed == 0) continue;

            List<UUID> targets = memberRepository.findUserIdsByConversationId(pin.getConversationId());

            String when = (pin.getStartAt() == null) ? "곧" : DT.format(pin.getStartAt());
            String place = (pin.getPlaceText() == null || pin.getPlaceText().isBlank()) ? "장소 미정" : pin.getPlaceText();
            String title = (pin.getTitle() == null || pin.getTitle().isBlank()) ? "약속" : pin.getTitle();

            String ahead;
            if (pin.getStartAt() == null || pin.getRemindAt() == null) {
                ahead = "리마인드";
            } else {
                long mins = Duration.between(pin.getRemindAt(), pin.getStartAt()).toMinutes();
                if (mins == 60) ahead = "1시간 전";
                else if (mins == 30) ahead = "30분 전";
                else if (mins == 10) ahead = "10분 전";
                else if (mins == 5) ahead = "5분 전";
                else ahead = mins + "분 전";
            }

            String body = "⏰ " + ahead + " · " + title + " · " + place + " · " + when;

            for (UUID userId : targets) {
                notificationCommandService.createIfNotExists(
                        userId,
                        NotificationType.PIN_REMIND,
                        pin.getId(),
                        body
                );
                sent++;
            }
        }

        log.info("🔔 PIN_REMIND scheduler processed | duePins={} notificationsAttempted={}", duePins.size(), sent);
    }
}