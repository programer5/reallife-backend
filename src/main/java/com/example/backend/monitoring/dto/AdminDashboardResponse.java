package com.example.backend.monitoring.dto;

import com.example.backend.domain.notification.NotificationType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record AdminDashboardResponse(
        HealthStatus status,
        String service,
        String version,
        List<String> activeProfiles,
        LocalDateTime generatedAt,
        Overview overview,
        Health health,
        Totals totals,
        Recent recent,
        Insights insights,
        List<String> notes
) {
    public record Overview(
            int activeSseConnections,
            long unreadNotifications,
            long activePins,
            long todayCreatedNotifications,
            long todayCreatedMessages,
            long todayCreatedPosts
    ) {
    }

    public record Health(
            Map<String, HealthStatus> checks,
            LocalDateTime lastSseEventSentAt,
            LocalDateTime lastReminderRunAt,
            LocalDateTime lastReminderSuccessAt,
            long recentReminderCreatedCount,
            long minutesSinceLastReminderRun,
            List<String> summaryNotes
    ) {
    }

    public record Totals(
            long users,
            long posts,
            long comments,
            long conversations,
            long messages,
            long activePins,
            long notifications
    ) {
    }

    public record Recent(
            List<RecentNotificationItem> notifications
    ) {
    }

    public record RecentNotificationItem(
            String id,
            String userId,
            NotificationType type,
            String body,
            boolean read,
            LocalDateTime createdAt
    ) {
    }

    public record Insights(
            List<NotificationTypeCount> notificationTypeCounts,
            String topNotificationType,
            String unreadPressure,
            String realtimeHealth,
            String reminderHealth,
            String opsFocusTitle,
            String opsFocusReason
    ) {
    }

    public record NotificationTypeCount(
            NotificationType type,
            long count,
            int ratio
    ) {
    }
}