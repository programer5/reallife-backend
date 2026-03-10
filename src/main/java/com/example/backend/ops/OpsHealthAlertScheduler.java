package com.example.backend.ops;

import com.example.backend.monitoring.dto.AdminHealthResponse;
import com.example.backend.monitoring.dto.RealtimeHealthResponse;
import com.example.backend.monitoring.dto.ReminderHealthResponse;
import com.example.backend.monitoring.service.AdminHealthService;
import com.example.backend.monitoring.service.RealtimeHealthService;
import com.example.backend.monitoring.service.ReminderHealthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OpsHealthAlertScheduler {

    private final AdminHealthService adminHealthService;
    private final RealtimeHealthService realtimeHealthService;
    private final ReminderHealthService reminderHealthService;
    private final OpsAlertService opsAlertService;

    @Scheduled(
            initialDelayString = "${ops.alert.health-check-initial-delay-ms:15000}",
            fixedDelayString = "${ops.alert.health-check-fixed-delay-ms:30000}"
    )
    public void checkAndAlert() {
        try {
            AdminHealthResponse adminHealth = adminHealthService.getAdminHealth();
            RealtimeHealthResponse realtime = realtimeHealthService.getRealtimeHealth();
            ReminderHealthResponse reminder = reminderHealthService.getReminderHealth();

            opsAlertService.sendAdminHealthAlert(adminHealth);
            opsAlertService.sendRealtimeHealthAlert(realtime);
            opsAlertService.sendReminderHealthAlert(reminder);
        } catch (Exception e) {
            log.error("Failed while checking ops health alerts", e);
        }
    }
}