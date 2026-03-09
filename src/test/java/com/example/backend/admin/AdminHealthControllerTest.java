package com.example.backend.controller.admin;

import com.example.backend.controller.DocsTestSupport;
import com.example.backend.monitoring.support.NotificationHealthTracker;
import com.example.backend.monitoring.support.ReminderHealthTracker;
import com.example.backend.monitoring.support.SseHealthTracker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static com.example.backend.domain.notification.NotificationType.MESSAGE_RECEIVED;
import static com.example.backend.domain.notification.NotificationType.PIN_REMIND;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
@Transactional
class AdminHealthControllerTest {

    @Autowired WebApplicationContext context;
    @Autowired DocsTestSupport docs;

    @Autowired SseHealthTracker sseHealthTracker;
    @Autowired ReminderHealthTracker reminderHealthTracker;
    @Autowired NotificationHealthTracker notificationHealthTracker;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        sseHealthTracker.reset();
        reminderHealthTracker.reset();
        notificationHealthTracker.reset();

        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void admin_health_인증없음_401() throws Exception {
        mockMvc.perform(get("/admin/health"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void admin_health_요약_응답_200() throws Exception {
        var admin = docs.saveUser("healthadmin", "운영자");
        String token = docs.issueTokenFor(admin);

        sseHealthTracker.onConnected();
        sseHealthTracker.onEventSent();
        reminderHealthTracker.markRunStarted();
        reminderHealthTracker.markRunSuccess(2);
        notificationHealthTracker.markCreated(MESSAGE_RECEIVED);
        notificationHealthTracker.markCreated(PIN_REMIND);

        mockMvc.perform(get("/admin/health")
                        .header(DocsTestSupport.headerName(), DocsTestSupport.auth(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.service").exists())
                .andExpect(jsonPath("$.checks.db").exists())
                .andExpect(jsonPath("$.checks.redis").exists())
                .andExpect(jsonPath("$.checks.sse").exists())
                .andExpect(jsonPath("$.checks.reminderScheduler").exists())
                .andExpect(jsonPath("$.metrics.activeSseConnections").value(1))
                .andExpect(jsonPath("$.metrics.lastReminderRunAt").exists())
                .andExpect(jsonPath("$.metrics.recentReminderCreatedCount").value(2));
    }

    @Test
    void admin_health_realtime_응답_200() throws Exception {
        var admin = docs.saveUser("realtimeadmin", "운영자");
        String token = docs.issueTokenFor(admin);

        sseHealthTracker.onConnected();
        sseHealthTracker.onEventSent();
        notificationHealthTracker.markCreated(MESSAGE_RECEIVED);
        notificationHealthTracker.markCreated(PIN_REMIND);

        mockMvc.perform(get("/admin/health/realtime")
                        .header(DocsTestSupport.headerName(), DocsTestSupport.auth(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.activeSseConnections").value(1))
                .andExpect(jsonPath("$.lastSseEventSentAt").exists())
                .andExpect(jsonPath("$.lastNotificationCreatedAt").exists())
                .andExpect(jsonPath("$.lastMessageNotificationCreatedAt").exists())
                .andExpect(jsonPath("$.lastPinRemindNotificationCreatedAt").exists());
    }

    @Test
    void admin_health_reminder_응답_200() throws Exception {
        var admin = docs.saveUser("reminderadmin", "운영자");
        String token = docs.issueTokenFor(admin);

        reminderHealthTracker.markRunStarted();
        reminderHealthTracker.markRunSuccess(5);

        mockMvc.perform(get("/admin/health/reminder")
                        .header(DocsTestSupport.headerName(), DocsTestSupport.auth(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.schedulerEnabled").value(true))
                .andExpect(jsonPath("$.lastRunAt").exists())
                .andExpect(jsonPath("$.lastSuccessAt").exists())
                .andExpect(jsonPath("$.recentCreatedCount").value(5));
    }
}