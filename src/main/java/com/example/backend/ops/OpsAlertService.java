package com.example.backend.ops;

import com.example.backend.monitoring.dto.AdminHealthResponse;
import com.example.backend.monitoring.dto.HealthStatus;
import com.example.backend.monitoring.dto.RealtimeHealthResponse;
import com.example.backend.monitoring.dto.ReminderHealthResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpsAlertService {

    private final SlackWebhookClient slackWebhookClient;

    private final Map<String, LocalDateTime> lastSentAtMap = new ConcurrentHashMap<>();

    @Value("${spring.application.name:reallife-backend}")
    private String appName;

    @Value("${info.app.version:unknown}")
    private String appVersion;

    @Value("${ops.alert.cooldown-minutes:10}")
    private long cooldownMinutes;

    public void sendUnhandledExceptionAlert(Exception e, HttpServletRequest request) {
        String path = request != null ? request.getRequestURI() : "unknown";
        String method = request != null ? request.getMethod() : "UNKNOWN";
        String key = "error:" + e.getClass().getName() + ":" + path;

        if (!shouldSend(key)) {
            return;
        }

        String title = "🚨 RealLife 서버 에러";
        String body = """
                서비스: %s (%s)
                메서드: %s
                경로: %s
                예외: %s
                메시지: %s
                발생시각: %s
                """.formatted(
                appName,
                appVersion,
                method,
                path,
                e.getClass().getSimpleName(),
                shorten(e.getMessage(), 500),
                LocalDateTime.now()
        );

        slackWebhookClient.send(title, body);
    }

    public void sendAdminHealthAlert(AdminHealthResponse adminHealth) {
        if (adminHealth == null) return;

        HealthStatus status = adminHealth.getStatus();
        if (status == null || status == HealthStatus.UP) {
            return;
        }

        String key = "health:admin:" + status;
        if (!shouldSend(key)) {
            return;
        }

        String title = status == HealthStatus.DOWN
                ? "🚨 RealLife 전체 헬스 DOWN"
                : "⚠️ RealLife 전체 헬스 DEGRADED";

        String body = """
                서비스: %s (%s)
                현재상태: %s
                체크: %s
                메모: %s
                서버시각: %s
                확인: /admin/health, /ops/dashboard
                """.formatted(
                appName,
                appVersion,
                status,
                adminHealth.getChecks(),
                adminHealth.getNotes(),
                adminHealth.getServerTime()
        );

        slackWebhookClient.send(title, body);
    }

    public void sendRealtimeHealthAlert(RealtimeHealthResponse realtime) {
        if (realtime == null) return;

        HealthStatus status = realtime.getStatus();
        if (status == null || status == HealthStatus.UP) {
            return;
        }

        String key = "health:realtime:" + status;
        if (!shouldSend(key)) {
            return;
        }

        String title = status == HealthStatus.DOWN
                ? "🚨 RealLife SSE 상태 DOWN"
                : "⚠️ RealLife SSE 상태 이상";

        String body = """
                서비스: %s (%s)
                현재상태: %s
                activeSseConnections: %s
                lastSseEventSentAt: %s
                lastNotificationCreatedAt: %s
                notes: %s
                확인: /admin/health/realtime, /ops/dashboard
                """.formatted(
                appName,
                appVersion,
                status,
                realtime.getActiveSseConnections(),
                realtime.getLastSseEventSentAt(),
                realtime.getLastNotificationCreatedAt(),
                realtime.getNotes()
        );

        slackWebhookClient.send(title, body);
    }

    public void sendReminderHealthAlert(ReminderHealthResponse reminder) {
        if (reminder == null) return;

        HealthStatus status = reminder.getStatus();
        if (status == null || status == HealthStatus.UP) {
            return;
        }

        String key = "health:reminder:" + status;
        if (!shouldSend(key)) {
            return;
        }

        String title = status == HealthStatus.DOWN
                ? "🚨 RealLife Reminder 상태 DOWN"
                : "⚠️ RealLife Reminder 지연 감지";

        String body = """
                서비스: %s (%s)
                현재상태: %s
                schedulerEnabled: %s
                lastRunAt: %s
                lastSuccessAt: %s
                minutesSinceLastRun: %s
                recentCreatedCount: %s
                notes: %s
                확인: /admin/health/reminder, /ops/dashboard
                """.formatted(
                appName,
                appVersion,
                status,
                reminder.isSchedulerEnabled(),
                reminder.getLastRunAt(),
                reminder.getLastSuccessAt(),
                reminder.getMinutesSinceLastRun(),
                reminder.getRecentCreatedCount(),
                reminder.getNotes()
        );

        slackWebhookClient.send(title, body);
    }

    private boolean shouldSend(String key) {
        if (!slackWebhookClient.isAvailable()) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime lastSentAt = lastSentAtMap.get(key);

        if (lastSentAt != null) {
            long minutes = Duration.between(lastSentAt, now).toMinutes();
            if (minutes < cooldownMinutes) {
                log.debug("Skip ops alert by cooldown. key={}, minutes={}", key, minutes);
                return false;
            }
        }

        lastSentAtMap.put(key, now);
        return true;
    }

    private String shorten(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return "(no message)";
        }
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "...";
    }
}