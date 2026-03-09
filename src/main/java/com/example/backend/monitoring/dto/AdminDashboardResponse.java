package com.example.backend.monitoring.dto;

import com.example.backend.domain.notification.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter
@Builder
@AllArgsConstructor
public class AdminDashboardResponse {

    private HealthStatus status;
    private String service;
    private String version;
    private List<String> activeProfiles;
    private LocalDateTime generatedAt;

    private Overview overview;
    private Health health;
    private Totals totals;
    private Recent recent;
    private Insights insights;
    private List<String> notes;

    @Getter
    @Builder
    @AllArgsConstructor
    public static class Overview {
        private int activeSseConnections;
        private long unreadNotifications;
        private long activePins;
        private long todayCreatedNotifications;
        private long todayCreatedMessages;
        private long todayCreatedPosts;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class Health {
        private Map<String, HealthStatus> checks;
        private LocalDateTime lastSseEventSentAt;
        private LocalDateTime lastReminderRunAt;
        private LocalDateTime lastReminderSuccessAt;
        private long recentReminderCreatedCount;
        private long minutesSinceLastReminderRun;
        private List<String> summaryNotes;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class Totals {
        private long users;
        private long posts;
        private long comments;
        private long conversations;
        private long messages;
        private long activePins;
        private long notifications;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class Recent {
        private List<RecentNotificationItem> notifications;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class RecentNotificationItem {
        private String id;
        private String userId;
        private NotificationType type;
        private String body;
        private boolean read;
        private LocalDateTime createdAt;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class Insights {
        private List<NotificationTypeCount> notificationTypeCounts;
        private String topNotificationType;
        private String unreadPressure;
        private String realtimeHealth;
        private String reminderHealth;
        private String opsFocusTitle;
        private String opsFocusReason;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class NotificationTypeCount {
        private NotificationType type;
        private long count;
        private int ratio;
    }
}