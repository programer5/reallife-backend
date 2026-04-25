package com.example.backend.admin;

import com.example.backend.controller.DocsTestSupport;
import com.example.backend.monitoring.support.NotificationHealthTracker;
import com.example.backend.monitoring.support.ReminderHealthTracker;
import com.example.backend.monitoring.support.SseHealthTracker;
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
class AdminHealthControllerDocsTest {

    @Autowired WebApplicationContext context;
    @Autowired DocsTestSupport docs;

    @Autowired SseHealthTracker sseHealthTracker;
    @Autowired ReminderHealthTracker reminderHealthTracker;
    @Autowired NotificationHealthTracker notificationHealthTracker;

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
    void admin_health_요약_문서화(RestDocumentationContextProvider restDocumentation) throws Exception {
        var admin = docs.saveUserExact("healthdoc@test.com", "healthdoc", "운영자");
        String token = docs.issueTokenFor(admin);

        sseHealthTracker.onConnected();
        sseHealthTracker.onEventSent();
        reminderHealthTracker.markRunStarted();
        reminderHealthTracker.markRunSuccess(2);
        notificationHealthTracker.markCreated(MESSAGE_RECEIVED);
        notificationHealthTracker.markCreated(PIN_REMIND);

        mockMvc(restDocumentation)
                .perform(get("/admin/health")
                        .header(HttpHeaders.AUTHORIZATION, DocsTestSupport.auth(token)))
                .andExpect(status().isOk())
                .andDo(document("admin-health-get",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(
                                headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer {accessToken} (ops allowlist 포함 계정)")
                        ),
                        relaxedResponseFields(
                                fieldWithPath("status").type(STRING).description("전체 운영 상태 (UP, DEGRADED, DOWN)"),
                                fieldWithPath("service").type(STRING).description("서비스 이름"),
                                fieldWithPath("version").type(STRING).description("애플리케이션 버전"),
                                fieldWithPath("activeProfiles").type(ARRAY).description("현재 활성 프로필"),
                                fieldWithPath("checks").type(OBJECT).description("핵심 운영 체크 결과"),
                                fieldWithPath("checks.db").type(STRING).description("DB 상태"),
                                fieldWithPath("checks.redis").type(STRING).description("Redis 상태"),
                                fieldWithPath("checks.sse").type(STRING).description("SSE 상태"),
                                fieldWithPath("checks.reminderScheduler").type(STRING).description("Reminder Scheduler 상태"),
                                fieldWithPath("metrics").type(OBJECT).description("운영 지표"),
                                fieldWithPath("metrics.activeSseConnections").type(NUMBER).description("현재 활성 SSE 연결 수"),
                                fieldWithPath("metrics.lastSseEventSentAt").optional().description("마지막 SSE 이벤트 전송 시각"),
                                fieldWithPath("metrics.lastReminderRunAt").optional().description("마지막 reminder scheduler 실행 시각"),
                                fieldWithPath("metrics.lastReminderSuccessAt").optional().description("마지막 reminder scheduler 성공 시각"),
                                fieldWithPath("metrics.lastReminderFailureAt").optional().description("마지막 reminder scheduler 실패 시각"),
                                fieldWithPath("metrics.recentReminderCreatedCount").type(NUMBER).description("최근 scheduler 실행에서 생성한 reminder 알림 수"),
                                fieldWithPath("notes").type(ARRAY).description("운영 참고 메모"),
                                fieldWithPath("notes[]").description("운영 참고 메모 항목"),
                                fieldWithPath("serverTime").description("서버 현재 시각")
                        )
                ));
    }

    @Test
    void admin_health_realtime_문서화(RestDocumentationContextProvider restDocumentation) throws Exception {
        var admin = docs.saveUserExact("realtimedoc@test.com", "realtimedoc", "운영자");
        String token = docs.issueTokenFor(admin);

        sseHealthTracker.onConnected();
        sseHealthTracker.onEventSent();
        notificationHealthTracker.markCreated(MESSAGE_RECEIVED);
        notificationHealthTracker.markCreated(PIN_REMIND);

        mockMvc(restDocumentation)
                .perform(get("/admin/health/realtime")
                        .header(HttpHeaders.AUTHORIZATION, DocsTestSupport.auth(token)))
                .andExpect(status().isOk())
                .andDo(document("admin-health-realtime-get",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(
                                headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer {accessToken} (ops allowlist 포함 계정)")
                        ),
                        relaxedResponseFields(
                                fieldWithPath("status").type(STRING).description("실시간 운영 상태"),
                                fieldWithPath("activeSseConnections").type(NUMBER).description("현재 활성 SSE 연결 수"),
                                fieldWithPath("lastSseEventSentAt").optional().description("마지막 SSE 이벤트 전송 시각"),
                                fieldWithPath("lastNotificationCreatedAt").optional().description("마지막 알림 생성 시각"),
                                fieldWithPath("lastMessageNotificationCreatedAt").optional().description("마지막 MESSAGE_RECEIVED 알림 생성 시각"),
                                fieldWithPath("lastPinRemindNotificationCreatedAt").optional().description("마지막 PIN_REMIND 알림 생성 시각"),
                                fieldWithPath("notes").type(ARRAY).description("운영 참고 메모"),
                                fieldWithPath("notes[]").description("운영 참고 메모 항목"),
                                fieldWithPath("serverTime").description("서버 현재 시각")
                        )
                ));
    }

    @Test
    void admin_health_reminder_문서화(RestDocumentationContextProvider restDocumentation) throws Exception {
        var admin = docs.saveUserExact("reminderdoc@test.com", "reminderdoc", "운영자");
        String token = docs.issueTokenFor(admin);

        reminderHealthTracker.markRunStarted();
        reminderHealthTracker.markRunSuccess(5);

        mockMvc(restDocumentation)
                .perform(get("/admin/health/reminder")
                        .header(HttpHeaders.AUTHORIZATION, DocsTestSupport.auth(token)))
                .andExpect(status().isOk())
                .andDo(document("admin-health-reminder-get",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(
                                headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer {accessToken} (ops allowlist 포함 계정)")
                        ),
                        relaxedResponseFields(
                                fieldWithPath("status").type(STRING).description("Reminder 운영 상태"),
                                fieldWithPath("schedulerEnabled").type(BOOLEAN).description("Scheduler 활성 여부"),
                                fieldWithPath("lastRunAt").optional().description("마지막 scheduler 실행 시각"),
                                fieldWithPath("lastSuccessAt").optional().description("마지막 scheduler 성공 시각"),
                                fieldWithPath("lastFailureAt").optional().description("마지막 scheduler 실패 시각"),
                                fieldWithPath("lastFailureMessage").optional().description("마지막 scheduler 실패 메시지"),
                                fieldWithPath("recentCreatedCount").type(NUMBER).description("최근 scheduler 실행에서 생성한 알림 수"),
                                fieldWithPath("minutesSinceLastRun").type(NUMBER).description("마지막 실행 이후 경과 분"),
                                fieldWithPath("notes").type(ARRAY).description("운영 참고 메모"),
                                fieldWithPath("notes[]").description("운영 참고 메모 항목"),
                                fieldWithPath("serverTime").description("서버 현재 시각")
                        )
                ));
    }
}