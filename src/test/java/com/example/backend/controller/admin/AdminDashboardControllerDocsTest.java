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
import org.springframework.http.HttpHeaders;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.UUID;

import static com.example.backend.domain.notification.NotificationType.MESSAGE_RECEIVED;
import static com.example.backend.domain.notification.NotificationType.PIN_REMIND;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.JsonFieldType.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.relaxedResponseFields;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@ExtendWith({RestDocumentationExtension.class, SpringExtension.class})
@Transactional
class AdminDashboardControllerDocsTest {

    @Autowired WebApplicationContext context;
    @Autowired DocsTestSupport docs;

    @Autowired SseHealthTracker sseHealthTracker;
    @Autowired ReminderHealthTracker reminderHealthTracker;
    @Autowired NotificationHealthTracker notificationHealthTracker;
    @Autowired NotificationCommandService notificationCommandService;

    @BeforeEach
    void setUp() {
        sseHealthTracker.reset();
        reminderHealthTracker.reset();
        notificationHealthTracker.reset();
    }

    private MockMvc mockMvc(RestDocumentationContextProvider restDocumentation) {
        return MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .apply(documentationConfiguration(restDocumentation))
                .build();
    }

    @Test
    void admin_dashboard_문서화(RestDocumentationContextProvider restDocumentation) throws Exception {
        var admin = docs.saveUserExact("dashboarddoc@test.com", "dashboarddoc", "운영자");
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
                "운영 대시보드 문서화용 메시지 알림"
        );
        notificationCommandService.createIfNotExists(
                admin.getId(),
                PIN_REMIND,
                UUID.randomUUID(),
                UUID.randomUUID(),
                "운영 대시보드 문서화용 리마인더 알림"
        );

        mockMvc(restDocumentation)
                .perform(get("/admin/dashboard")
                        .header(HttpHeaders.AUTHORIZATION, DocsTestSupport.auth(token)))
                .andExpect(status().isOk())
                .andDo(document("admin-dashboard-get",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(
                                headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer {accessToken} (ops allowlist 포함 계정)")
                        ),
                        relaxedResponseFields(
                                fieldWithPath("status").type(STRING).description("대시보드 전체 상태 (UP, DEGRADED, DOWN)"),
                                fieldWithPath("service").type(STRING).description("서비스 이름"),
                                fieldWithPath("version").type(STRING).description("애플리케이션 버전"),
                                fieldWithPath("activeProfiles").type(ARRAY).description("현재 활성 프로필"),
                                fieldWithPath("generatedAt").description("대시보드 생성 시각"),

                                fieldWithPath("overview").type(OBJECT).description("운영 요약"),
                                fieldWithPath("overview.activeSseConnections").type(NUMBER).description("현재 활성 SSE 연결 수"),
                                fieldWithPath("overview.unreadNotifications").type(NUMBER).description("전체 미읽음 알림 수"),
                                fieldWithPath("overview.activePins").type(NUMBER).description("ACTIVE 상태 핀 수"),
                                fieldWithPath("overview.todayCreatedNotifications").type(NUMBER).description("최근 24시간 생성 알림 수"),
                                fieldWithPath("overview.todayCreatedMessages").type(NUMBER).description("최근 24시간 생성 메시지 수"),
                                fieldWithPath("overview.todayCreatedPosts").type(NUMBER).description("최근 24시간 생성 게시글 수"),

                                fieldWithPath("health").type(OBJECT).description("운영 health 상세"),
                                fieldWithPath("health.checks").type(OBJECT).description("핵심 health 상태"),
                                fieldWithPath("health.lastSseEventSentAt").optional().description("마지막 SSE 이벤트 전송 시각"),
                                fieldWithPath("health.lastReminderRunAt").optional().description("마지막 reminder scheduler 실행 시각"),
                                fieldWithPath("health.lastReminderSuccessAt").optional().description("마지막 reminder scheduler 성공 시각"),
                                fieldWithPath("health.recentReminderCreatedCount").type(NUMBER).description("최근 reminder 생성 수"),
                                fieldWithPath("health.minutesSinceLastReminderRun").type(NUMBER).description("마지막 reminder 실행 이후 경과 분"),
                                fieldWithPath("health.summaryNotes").type(ARRAY).description("health 요약 메모"),
                                fieldWithPath("health.summaryNotes[]").description("health 요약 메모 항목"),

                                fieldWithPath("totals").type(OBJECT).description("누적 규모 지표"),
                                fieldWithPath("totals.users").type(NUMBER).description("전체 사용자 수"),
                                fieldWithPath("totals.posts").type(NUMBER).description("전체 게시글 수"),
                                fieldWithPath("totals.comments").type(NUMBER).description("전체 댓글 수"),
                                fieldWithPath("totals.conversations").type(NUMBER).description("전체 대화방 수"),
                                fieldWithPath("totals.messages").type(NUMBER).description("전체 메시지 수"),
                                fieldWithPath("totals.activePins").type(NUMBER).description("ACTIVE 상태 핀 수"),
                                fieldWithPath("totals.notifications").type(NUMBER).description("전체 알림 수"),

                                fieldWithPath("recent").type(OBJECT).description("최근 항목 요약"),
                                fieldWithPath("recent.notifications").type(ARRAY).description("최근 알림 목록"),
                                fieldWithPath("recent.notifications[].id").type(STRING).description("알림 ID"),
                                fieldWithPath("recent.notifications[].userId").type(STRING).description("알림 대상 사용자 ID"),
                                fieldWithPath("recent.notifications[].type").type(STRING).description("알림 타입"),
                                fieldWithPath("recent.notifications[].body").type(STRING).description("알림 본문"),
                                fieldWithPath("recent.notifications[].read").type(BOOLEAN).description("읽음 여부"),
                                fieldWithPath("recent.notifications[].createdAt").description("생성 시각"),

                                fieldWithPath("insights").type(OBJECT).description("운영 인사이트"),
                                fieldWithPath("insights.notificationTypeCounts").type(ARRAY).description("최근 알림 타입 집계"),
                                fieldWithPath("insights.notificationTypeCounts[].type").type(STRING).description("알림 타입"),
                                fieldWithPath("insights.notificationTypeCounts[].count").type(NUMBER).description("해당 타입 개수"),
                                fieldWithPath("insights.notificationTypeCounts[].ratio").type(NUMBER).description("최근 알림 내 비율(%)"),
                                fieldWithPath("insights.topNotificationType").type(STRING).description("가장 우세한 최근 알림 타입"),
                                fieldWithPath("insights.unreadPressure").type(STRING).description("미읽음 알림 압력 (LOW, MEDIUM, HIGH)"),
                                fieldWithPath("insights.realtimeHealth").type(STRING).description("실시간 상태 요약"),
                                fieldWithPath("insights.reminderHealth").type(STRING).description("Reminder 상태 요약"),
                                fieldWithPath("insights.opsFocusTitle").type(STRING).description("현재 운영 우선 포커스 제목"),
                                fieldWithPath("insights.opsFocusReason").type(STRING).description("현재 운영 우선 포커스 이유"),

                                fieldWithPath("notes").type(ARRAY).description("운영 참고 메모"),
                                fieldWithPath("notes[]").description("운영 참고 메모 항목")
                        )
                ));
    }
}