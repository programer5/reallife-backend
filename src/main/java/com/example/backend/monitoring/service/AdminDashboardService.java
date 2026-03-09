package com.example.backend.monitoring.service;

import com.example.backend.domain.notification.Notification;
import com.example.backend.domain.notification.NotificationType;
import com.example.backend.domain.pin.PinStatus;
import com.example.backend.monitoring.dto.AdminDashboardResponse;
import com.example.backend.monitoring.dto.AdminHealthResponse;
import com.example.backend.monitoring.dto.HealthStatus;
import com.example.backend.monitoring.dto.RealtimeHealthResponse;
import com.example.backend.monitoring.dto.ReminderHealthResponse;
import com.example.backend.repository.comment.CommentRepository;
import com.example.backend.repository.message.ConversationRepository;
import com.example.backend.repository.message.MessageRepository;
import com.example.backend.repository.notification.NotificationRepository;
import com.example.backend.repository.pin.ConversationPinRepository;
import com.example.backend.repository.post.PostRepository;
import com.example.backend.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private final Environment environment;
    private final AdminHealthService adminHealthService;
    private final RealtimeHealthService realtimeHealthService;
    private final ReminderHealthService reminderHealthService;

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final ConversationPinRepository conversationPinRepository;
    private final NotificationRepository notificationRepository;

    @Value("${spring.application.name:reallife-backend}")
    private String appName;

    @Value("${info.app.version:unknown}")
    private String appVersion;

    public AdminDashboardResponse getDashboard() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime since24h = now.minusHours(24);

        AdminHealthResponse adminHealth = adminHealthService.getAdminHealth();
        RealtimeHealthResponse realtime = realtimeHealthService.getRealtimeHealth();
        ReminderHealthResponse reminder = reminderHealthService.getReminderHealth();

        long users = userRepository.countByDeletedFalse();
        long posts = postRepository.countByDeletedFalse();
        long comments = commentRepository.countByDeletedFalse();
        long conversations = conversationRepository.countByDeletedFalse();
        long messages = messageRepository.countByDeletedFalse();
        long activePins = conversationPinRepository.countByDeletedFalseAndStatus(PinStatus.ACTIVE);
        long notifications = notificationRepository.countByDeletedFalse();

        long unreadNotifications = notificationRepository.countByReadAtIsNullAndDeletedFalse();
        long todayCreatedNotifications = notificationRepository.countByDeletedFalseAndCreatedAtAfter(since24h);
        long todayCreatedMessages = messageRepository.countByDeletedFalseAndCreatedAtAfter(since24h);
        long todayCreatedPosts = postRepository.countByDeletedFalseAndCreatedAtAfter(since24h);

        List<Notification> recentNotificationEntities =
                notificationRepository.findTop10ByDeletedFalseOrderByCreatedAtDesc();

        List<AdminDashboardResponse.RecentNotificationItem> recentNotifications =
                recentNotificationEntities.stream()
                        .map(this::toRecentNotificationItem)
                        .toList();

        Map<String, HealthStatus> checks = new LinkedHashMap<>();
        if (adminHealth.getChecks() != null) {
            checks.putAll(adminHealth.getChecks());
        }

        List<AdminDashboardResponse.NotificationTypeCount> notificationTypeCounts =
                buildNotificationTypeCounts(recentNotificationEntities);

        String topNotificationType = notificationTypeCounts.isEmpty()
                ? "NO_DATA"
                : String.valueOf(notificationTypeCounts.get(0).getType());

        String unreadPressure = unreadPressure(unreadNotifications);
        String realtimeHealth = realtimeHealth(realtime.getActiveSseConnections());
        String reminderHealth = reminderHealth(reminder.getMinutesSinceLastRun());

        String opsFocusTitle = opsFocusTitle(
                adminHealth.getStatus(),
                realtime.getActiveSseConnections(),
                reminder.getMinutesSinceLastRun(),
                unreadNotifications
        );
        String opsFocusReason = opsFocusReason(
                adminHealth.getStatus(),
                realtime.getActiveSseConnections(),
                reminder.getMinutesSinceLastRun(),
                unreadNotifications
        );

        List<String> healthSummaryNotes = buildHealthSummaryNotes(
                adminHealth.getStatus(),
                checks,
                realtime.getActiveSseConnections(),
                reminder.getMinutesSinceLastRun(),
                unreadNotifications,
                recentReminderCreatedCount(reminder)
        );

        List<String> notes = buildDashboardNotes(
                topNotificationType,
                unreadPressure,
                realtimeHealth,
                reminderHealth,
                todayCreatedNotifications,
                activePins
        );

        return AdminDashboardResponse.builder()
                .status(adminHealth.getStatus() == null ? HealthStatus.DEGRADED : adminHealth.getStatus())
                .service(appName)
                .version(appVersion)
                .activeProfiles(List.of(environment.getActiveProfiles()))
                .generatedAt(now)

                .overview(AdminDashboardResponse.Overview.builder()
                        .activeSseConnections(realtime.getActiveSseConnections())
                        .unreadNotifications(unreadNotifications)
                        .activePins(activePins)
                        .todayCreatedNotifications(todayCreatedNotifications)
                        .todayCreatedMessages(todayCreatedMessages)
                        .todayCreatedPosts(todayCreatedPosts)
                        .build())

                .health(AdminDashboardResponse.Health.builder()
                        .checks(checks)
                        .lastSseEventSentAt(realtime.getLastSseEventSentAt())
                        .lastReminderRunAt(reminder.getLastRunAt())
                        .lastReminderSuccessAt(reminder.getLastSuccessAt())
                        .recentReminderCreatedCount(recentReminderCreatedCount(reminder))
                        .minutesSinceLastReminderRun(reminder.getMinutesSinceLastRun())
                        .summaryNotes(healthSummaryNotes)
                        .build())

                .totals(AdminDashboardResponse.Totals.builder()
                        .users(users)
                        .posts(posts)
                        .comments(comments)
                        .conversations(conversations)
                        .messages(messages)
                        .activePins(activePins)
                        .notifications(notifications)
                        .build())

                .recent(AdminDashboardResponse.Recent.builder()
                        .notifications(recentNotifications)
                        .build())

                .insights(AdminDashboardResponse.Insights.builder()
                        .notificationTypeCounts(notificationTypeCounts)
                        .topNotificationType(topNotificationType)
                        .unreadPressure(unreadPressure)
                        .realtimeHealth(realtimeHealth)
                        .reminderHealth(reminderHealth)
                        .opsFocusTitle(opsFocusTitle)
                        .opsFocusReason(opsFocusReason)
                        .build())

                .notes(notes)
                .build();
    }

    private long recentReminderCreatedCount(ReminderHealthResponse reminder) {
        return reminder.getRecentCreatedCount();
    }

    private AdminDashboardResponse.RecentNotificationItem toRecentNotificationItem(Notification notification) {
        return AdminDashboardResponse.RecentNotificationItem.builder()
                .id(notification.getId().toString())
                .userId(notification.getUserId().toString())
                .type(notification.getType())
                .body(notification.getBody())
                .read(notification.isRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }

    private List<AdminDashboardResponse.NotificationTypeCount> buildNotificationTypeCounts(List<Notification> recentNotifications) {
        if (recentNotifications == null || recentNotifications.isEmpty()) {
            return List.of();
        }

        Map<NotificationType, Long> counter = new LinkedHashMap<>();
        for (Notification notification : recentNotifications) {
            NotificationType type = notification.getType();
            counter.put(type, counter.getOrDefault(type, 0L) + 1L);
        }

        int total = recentNotifications.size();

        return counter.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .map(entry -> AdminDashboardResponse.NotificationTypeCount.builder()
                        .type(entry.getKey())
                        .count(entry.getValue())
                        .ratio((int) Math.round((entry.getValue() * 100.0) / total))
                        .build())
                .toList();
    }

    private String unreadPressure(long unreadNotifications) {
        if (unreadNotifications >= 50) return "HIGH";
        if (unreadNotifications >= 10) return "MEDIUM";
        return "LOW";
    }

    private String realtimeHealth(int activeSseConnections) {
        return activeSseConnections == 0 ? "WATCH" : "GOOD";
    }

    private String reminderHealth(long minutesSinceLastRun) {
        if (minutesSinceLastRun > 15) return "DELAYED";
        if (minutesSinceLastRun > 5) return "WATCH";
        return "GOOD";
    }

    private String opsFocusTitle(
            HealthStatus status,
            int activeSseConnections,
            long minutesSinceLastReminderRun,
            long unreadNotifications
    ) {
        if (status != HealthStatus.UP) return "서비스 상태 확인";
        if (activeSseConnections == 0) return "SSE 연결 확인";
        if (minutesSinceLastReminderRun > 15) return "Reminder Scheduler 확인";
        if (unreadNotifications >= 10) return "미읽음 알림 흐름 확인";
        return "정상 운영 중";
    }

    private String opsFocusReason(
            HealthStatus status,
            int activeSseConnections,
            long minutesSinceLastReminderRun,
            long unreadNotifications
    ) {
        if (status != HealthStatus.UP) return "health 상태가 UP이 아닙니다.";
        if (activeSseConnections == 0) return "실시간 수신 연결이 없습니다.";
        if (minutesSinceLastReminderRun > 15) return "scheduler 지연이 감지됐습니다.";
        if (unreadNotifications >= 10) return "미읽음 알림이 누적되고 있습니다.";
        return "이상 징후 없이 안정적으로 동작하고 있습니다.";
    }

    private List<String> buildHealthSummaryNotes(
            HealthStatus status,
            Map<String, HealthStatus> checks,
            int activeSseConnections,
            long minutesSinceLastReminderRun,
            long unreadNotifications,
            long recentReminderCreatedCount
    ) {
        List<String> notes = new ArrayList<>();

        notes.add("전체 health 상태: " + (status == null ? "UNKNOWN" : status));

        if (checks != null && !checks.isEmpty()) {
            List<String> degradedOrDown = checks.entrySet().stream()
                    .filter(e -> e.getValue() == HealthStatus.DEGRADED || e.getValue() == HealthStatus.DOWN)
                    .map(Map.Entry::getKey)
                    .toList();

            if (degradedOrDown.isEmpty()) {
                notes.add("DB / Redis / SSE / Reminder Scheduler 핵심 체크가 모두 정상입니다.");
            } else {
                notes.add("주의가 필요한 health 항목: " + String.join(", ", degradedOrDown));
            }
        }

        if (activeSseConnections == 0) {
            notes.add("현재 활성 SSE 연결이 없습니다. 실시간 흐름을 점검하세요.");
        } else {
            notes.add("활성 SSE 연결 수: " + activeSseConnections);
        }

        if (minutesSinceLastReminderRun > 15) {
            notes.add("Reminder Scheduler 지연: 마지막 실행 이후 " + minutesSinceLastReminderRun + "분 경과");
        } else {
            notes.add("Reminder Scheduler 실행 간격 정상");
        }

        notes.add("미읽음 알림 수: " + unreadNotifications);
        notes.add("최근 Reminder 생성 수: " + recentReminderCreatedCount);

        return notes;
    }

    private List<String> buildDashboardNotes(
            String topNotificationType,
            String unreadPressure,
            String realtimeHealth,
            String reminderHealth,
            long todayCreatedNotifications,
            long activePins
    ) {
        List<String> notes = new ArrayList<>();
        notes.add("Admin Dashboard 2차: 운영 상태 + 규모 + 최근 알림 + notification type 통계");
        notes.add("최근 가장 우세한 알림 타입: " + topNotificationType);
        notes.add("Unread Pressure: " + unreadPressure + ", Realtime Health: " + realtimeHealth + ", Reminder Health: " + reminderHealth);
        notes.add("최근 24시간 생성 알림 수: " + todayCreatedNotifications);
        notes.add("현재 ACTIVE 핀 수: " + activePins);
        notes.add("세부 health는 /admin/health, /admin/health/realtime, /admin/health/reminder 에서 확인");

        return notes;
    }
}