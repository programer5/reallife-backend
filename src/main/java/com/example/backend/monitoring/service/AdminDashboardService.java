package com.example.backend.monitoring.service;

import com.example.backend.domain.notification.Notification;
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
import java.util.List;
import java.util.stream.Collectors;

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
        LocalDateTime since = now.minusHours(24);

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
        long todayCreatedNotifications = notificationRepository.countByDeletedFalseAndCreatedAtAfter(since);
        long todayCreatedMessages = messageRepository.countByDeletedFalseAndCreatedAtAfter(since);
        long todayCreatedPosts = postRepository.countByDeletedFalseAndCreatedAtAfter(since);

        List<AdminDashboardResponse.RecentNotificationItem> recentNotifications =
                notificationRepository.findTop10ByDeletedFalseOrderByCreatedAtDesc().stream()
                        .map(this::toRecentNotificationItem)
                        .collect(Collectors.toList());

        return AdminDashboardResponse.builder()
                .status(adminHealth.getStatus())
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
                        .checks(adminHealth.getChecks())
                        .lastSseEventSentAt(realtime.getLastSseEventSentAt())
                        .lastReminderRunAt(reminder.getLastRunAt())
                        .lastReminderSuccessAt(reminder.getLastSuccessAt())
                        .recentReminderCreatedCount(reminder.getRecentCreatedCount())
                        .minutesSinceLastReminderRun(reminder.getMinutesSinceLastRun())
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
                .notes(List.of(
                        "Admin Dashboard 1차: 운영 상태 + 규모 + 최근 알림 요약",
                        "세부 health는 /admin/health, /admin/health/realtime, /admin/health/reminder 에서 확인"
                ))
                .build();
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
}