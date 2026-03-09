package com.example.backend.controller.admin;

import com.example.backend.controller.DocsTestSupport;
import com.example.backend.monitoring.support.NotificationHealthTracker;
import com.example.backend.monitoring.support.ReminderHealthTracker;
import com.example.backend.monitoring.support.SseHealthTracker;
import com.example.backend.service.notification.NotificationCommandService;
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

import java.util.UUID;

import static com.example.backend.domain.notification.NotificationType.MESSAGE_RECEIVED;
import static com.example.backend.domain.notification.NotificationType.PIN_REMIND;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
@Transactional
class AdminDashboardControllerTest {

    @Autowired WebApplicationContext context;
    @Autowired DocsTestSupport docs;

    @Autowired SseHealthTracker sseHealthTracker;
    @Autowired ReminderHealthTracker reminderHealthTracker;
    @Autowired NotificationHealthTracker notificationHealthTracker;
    @Autowired NotificationCommandService notificationCommandService;

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
    void admin_dashboard_인증없음_401() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void admin_dashboard_응답구조_200() throws Exception {
        var admin = docs.saveUser("dashboardadmin", "운영자");
        String token = docs.issueTokenFor(admin);

        sseHealthTracker.onConnected();
        sseHealthTracker.onEventSent();
        reminderHealthTracker.markRunStarted();
        reminderHealthTracker.markRunSuccess(3);
        notificationHealthTracker.markCreated(MESSAGE_RECEIVED);
        notificationHealthTracker.markCreated(PIN_REMIND);

        notificationCommandService.createIfNotExists(
                admin.getId(),
                MESSAGE_RECEIVED,
                UUID.randomUUID(),
                UUID.randomUUID(),
                "대시보드 테스트용 메시지 알림"
        );
        notificationCommandService.createIfNotExists(
                admin.getId(),
                PIN_REMIND,
                UUID.randomUUID(),
                UUID.randomUUID(),
                "대시보드 테스트용 리마인더 알림"
        );

        mockMvc.perform(get("/admin/dashboard")
                        .header(DocsTestSupport.headerName(), DocsTestSupport.auth(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.service").exists())
                .andExpect(jsonPath("$.version").exists())
                .andExpect(jsonPath("$.generatedAt").exists())

                .andExpect(jsonPath("$.overview").exists())
                .andExpect(jsonPath("$.overview.activeSseConnections").exists())
                .andExpect(jsonPath("$.overview.unreadNotifications").exists())
                .andExpect(jsonPath("$.overview.activePins").exists())
                .andExpect(jsonPath("$.overview.todayCreatedNotifications").exists())
                .andExpect(jsonPath("$.overview.todayCreatedMessages").exists())
                .andExpect(jsonPath("$.overview.todayCreatedPosts").exists())

                .andExpect(jsonPath("$.health").exists())
                .andExpect(jsonPath("$.health.checks").exists())
                .andExpect(jsonPath("$.health.recentReminderCreatedCount").exists())
                .andExpect(jsonPath("$.health.minutesSinceLastReminderRun").exists())
                .andExpect(jsonPath("$.health.summaryNotes").isArray())

                .andExpect(jsonPath("$.totals").exists())
                .andExpect(jsonPath("$.totals.users").exists())
                .andExpect(jsonPath("$.totals.posts").exists())
                .andExpect(jsonPath("$.totals.comments").exists())
                .andExpect(jsonPath("$.totals.conversations").exists())
                .andExpect(jsonPath("$.totals.messages").exists())
                .andExpect(jsonPath("$.totals.activePins").exists())
                .andExpect(jsonPath("$.totals.notifications").exists())

                .andExpect(jsonPath("$.recent").exists())
                .andExpect(jsonPath("$.recent.notifications").isArray())

                .andExpect(jsonPath("$.insights").exists())
                .andExpect(jsonPath("$.insights.notificationTypeCounts").isArray())
                .andExpect(jsonPath("$.insights.topNotificationType").exists())
                .andExpect(jsonPath("$.insights.unreadPressure").exists())
                .andExpect(jsonPath("$.insights.realtimeHealth").exists())
                .andExpect(jsonPath("$.insights.reminderHealth").exists())
                .andExpect(jsonPath("$.insights.opsFocusTitle").exists())
                .andExpect(jsonPath("$.insights.opsFocusReason").exists())

                .andExpect(jsonPath("$.notes").isArray());
    }
}