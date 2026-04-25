package com.example.backend.ops;

import com.example.backend.controller.admin.dto.AdminAlertTestResponse;
import com.example.backend.controller.admin.dto.OpsAlertHistoryItemResponse;
import com.example.backend.controller.admin.dto.OpsAlertHistoryResponse;
import com.example.backend.domain.error.OpsAlertLog;
import com.example.backend.monitoring.dto.AdminHealthResponse;
import com.example.backend.monitoring.dto.HealthStatus;
import com.example.backend.monitoring.dto.RealtimeHealthResponse;
import com.example.backend.monitoring.dto.ReminderHealthResponse;
import com.example.backend.repository.error.OpsAlertLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpsAlertService {

    private static final String CHANNEL = "SLACK";

    private final SlackWebhookClient slackWebhookClient;
    private final OpsAlertLogRepository opsAlertLogRepository;

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

        sendAndLog(key, title, body, "DANGER", null);
    }

    public void sendAdminHealthAlert(AdminHealthResponse adminHealth) {
        if (adminHealth == null) {
            return;
        }

        HealthStatus status = adminHealth.status();
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
                adminHealth.checks(),
                adminHealth.notes(),
                adminHealth.serverTime()
        );

        sendAndLog(key, title, body, status == HealthStatus.DOWN ? "DANGER" : "WARNING", null);
    }

    public void sendRealtimeHealthAlert(RealtimeHealthResponse realtime) {
        if (realtime == null) {
            return;
        }

        HealthStatus status = realtime.status();
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
                lastSseFailureAt: %s
                sseFailureCount: %s
                lastNotificationCreatedAt: %s
                notes: %s
                확인: /admin/health/realtime, /ops/dashboard
                """.formatted(
                appName,
                appVersion,
                status,
                realtime.activeSseConnections(),
                realtime.lastSseEventSentAt(),
                realtime.lastSseFailureAt(),
                realtime.sseFailureCount(),
                realtime.lastNotificationCreatedAt(),
                realtime.notes()
        );

        sendAndLog(key, title, body, status == HealthStatus.DOWN ? "DANGER" : "WARNING", null);
    }

    public void sendReminderHealthAlert(ReminderHealthResponse reminder) {
        if (reminder == null) {
            return;
        }

        HealthStatus status = reminder.status();
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
                lastFailureAt: %s
                minutesSinceLastRun: %s
                recentCreatedCount: %s
                notes: %s
                확인: /admin/health/reminder, /ops/dashboard
                """.formatted(
                appName,
                appVersion,
                status,
                reminder.schedulerEnabled(),
                reminder.lastRunAt(),
                reminder.lastSuccessAt(),
                reminder.lastFailureAt(),
                reminder.minutesSinceLastRun(),
                reminder.recentCreatedCount(),
                reminder.notes()
        );

        sendAndLog(key, title, body, status == HealthStatus.DOWN ? "DANGER" : "WARNING", null);
    }

    public AdminAlertTestResponse sendSlackTestAlert(String requestedBy) {
        boolean enabled = slackWebhookClient.isEnabled();
        boolean webhookConfigured = slackWebhookClient.hasWebhookConfigured();

        String title = "🧪 RealLife Slack 테스트";
        String body = """
                서비스: %s (%s)
                요청자: %s
                발생시각: %s

                이 메시지가 보이면 RealLife 운영 Slack webhook 연결은 정상입니다.
                다음 확인 위치:
                - /admin/dashboard
                - /admin/health
                - /admin/errors
                """.formatted(
                appName,
                appVersion,
                requestedBy,
                LocalDateTime.now()
        );

        boolean sent = sendAndLog("manual:test", title, body, "INFO", requestedBy);

        String message;
        if (sent) {
            message = "Slack 테스트 알림을 전송했습니다.";
        } else if (!enabled) {
            message = "Slack 알림이 비활성화되어 있습니다. OPS_ALERT_ENABLED=true 를 확인하세요.";
        } else if (!webhookConfigured) {
            message = "Slack webhook URL이 비어 있습니다. OPS_ALERT_SLACK_WEBHOOK_URL 을 확인하세요.";
        } else {
            message = "Slack 테스트 알림 전송에 실패했습니다. 서버 로그를 확인하세요.";
        }

        return new AdminAlertTestResponse(
                enabled,
                webhookConfigured,
                sent,
                CHANNEL,
                requestedBy,
                appName + " (" + appVersion + ")",
                message,
                LocalDateTime.now()
        );
    }

    public OpsAlertHistoryResponse getRecentAlertHistory() {
        List<OpsAlertHistoryItemResponse> items = opsAlertLogRepository.findTop20ByOrderByCreatedAtDesc().stream()
                .map(logEntity -> new OpsAlertHistoryItemResponse(
                        logEntity.getId(),
                        logEntity.getChannel(),
                        logEntity.getAlertKey(),
                        logEntity.getTitle(),
                        logEntity.getBody(),
                        logEntity.getLevel(),
                        logEntity.getStatus(),
                        logEntity.getRequestedBy(),
                        logEntity.getCreatedAt()
                ))
                .toList();

        return new OpsAlertHistoryResponse(items);
    }

    private boolean sendAndLog(String alertKey, String title, String body, String level, String requestedBy) {
        boolean sent = slackWebhookClient.send(title, body);

        try {
            opsAlertLogRepository.save(
                    OpsAlertLog.of(
                            CHANNEL,
                            alertKey,
                            title,
                            body,
                            level,
                            sent ? "SENT" : "FAILED",
                            requestedBy
                    )
            );
        } catch (Exception e) {
            log.error("Failed to persist ops alert log. alertKey={}", alertKey, e);
        }

        return sent;
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