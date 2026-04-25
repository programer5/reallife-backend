package com.example.backend.monitoring.service;

import com.example.backend.monitoring.dto.AdminHealthResponse;
import com.example.backend.monitoring.dto.HealthStatus;
import com.example.backend.monitoring.dto.RealtimeHealthResponse;
import com.example.backend.monitoring.dto.ReminderHealthResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminHealthService {

    private final Environment environment;
    private final RealtimeHealthService realtimeHealthService;
    private final ReminderHealthService reminderHealthService;
    private final DataSource dataSource;
    private final RedisConnectionFactory redisConnectionFactory;
    private final JdbcTemplate jdbcTemplate;

    @Value("${spring.application.name:reallife-backend}")
    private String appName;

    @Value("${info.app.version:unknown}")
    private String appVersion;

    public AdminHealthResponse getAdminHealth() {
        HealthStatus dbStatus = checkDb();
        HealthStatus redisStatus = checkRedis();

        RealtimeHealthResponse realtime = realtimeHealthService.getRealtimeHealth();
        ReminderHealthResponse reminder = reminderHealthService.getReminderHealth();

        Map<String, HealthStatus> checks = new LinkedHashMap<>();
        checks.put("db", dbStatus);
        checks.put("redis", redisStatus);
        checks.put("sse", realtime.status());
        checks.put("reminderScheduler", reminder.status());

        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("activeSseConnections", realtime.activeSseConnections());
        metrics.put("lastSseEventSentAt", realtime.lastSseEventSentAt());
        metrics.put("lastReminderRunAt", reminder.lastRunAt());
        metrics.put("lastReminderSuccessAt", reminder.lastSuccessAt());
        metrics.put("lastReminderFailureAt", reminder.lastFailureAt());
        metrics.put("recentReminderCreatedCount", reminder.recentCreatedCount());

        HealthStatus overall = aggregate(dbStatus, redisStatus, realtime.status(), reminder.status());

        return new AdminHealthResponse(
                overall,
                appName,
                appVersion,
                List.of(environment.getActiveProfiles()),
                checks,
                metrics,
                List.of(
                        "운영 핵심 항목: DB / Redis / SSE / Reminder Scheduler",
                        "상세 정보는 /admin/health/realtime, /admin/health/reminder 에서 확인"
                ),
                LocalDateTime.now()
        );
    }

    private HealthStatus checkDb() {
        try {
            jdbcTemplate.queryForObject("select 1", Integer.class);
            return HealthStatus.UP;
        } catch (Exception e) {
            return HealthStatus.DOWN;
        }
    }

    private HealthStatus checkRedis() {
        try {
            var conn = redisConnectionFactory.getConnection();
            String pong = conn.ping();
            return "PONG".equalsIgnoreCase(pong) ? HealthStatus.UP : HealthStatus.DEGRADED;
        } catch (Exception e) {
            return HealthStatus.DOWN;
        }
    }

    private HealthStatus aggregate(HealthStatus... statuses) {
        boolean hasDown = false;
        boolean hasDegraded = false;

        for (HealthStatus status : statuses) {
            if (status == HealthStatus.DOWN) hasDown = true;
            if (status == HealthStatus.DEGRADED) hasDegraded = true;
        }

        if (hasDown) return HealthStatus.DOWN;
        if (hasDegraded) return HealthStatus.DEGRADED;
        return HealthStatus.UP;
    }
}