package com.example.backend.controller.home;

import com.example.backend.controller.DocsTestSupport;
import com.example.backend.domain.notification.Notification;
import com.example.backend.domain.notification.NotificationType;
import com.example.backend.domain.pin.ConversationPin;
import com.example.backend.repository.notification.NotificationRepository;
import com.example.backend.repository.pin.ConversationPinRepository;
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

import java.time.LocalDateTime;
import java.util.UUID;

import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.JsonFieldType.BOOLEAN;
import static org.springframework.restdocs.payload.JsonFieldType.NULL;
import static org.springframework.restdocs.payload.JsonFieldType.NUMBER;
import static org.springframework.restdocs.payload.JsonFieldType.STRING;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@ExtendWith({RestDocumentationExtension.class, SpringExtension.class})
@Transactional
class HomeReminderSummaryControllerDocsTest {

    @Autowired WebApplicationContext context;
    @Autowired DocsTestSupport docs;
    @Autowired NotificationRepository notificationRepository;
    @Autowired ConversationPinRepository pinRepository;

    private MockMvc mockMvc(RestDocumentationContextProvider restDocumentation) {
        return MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .apply(documentationConfiguration(restDocumentation))
                .build();
    }

    @Test
    void 홈_리마인더_요약_조회_200(RestDocumentationContextProvider restDocumentation) throws Exception {
        var me = docs.saveUser("home-reminder", "나");
        String token = docs.issueTokenFor(me);
        UUID conversationId = UUID.randomUUID();
        UUID sourceMessageId = UUID.randomUUID();
        ConversationPin pin = pinRepository.saveAndFlush(ConversationPin.createSchedule(
                conversationId,
                me.getId(),
                sourceMessageId,
                "운동 약속",
                "체육관",
                LocalDateTime.now().withHour(19).withMinute(0).withSecond(0).withNano(0)
        ));

        Notification remind = notificationRepository.saveAndFlush(
                Notification.create(me.getId(), NotificationType.PIN_REMIND, pin.getId(), sourceMessageId, "오늘 19:00 운동 약속이 있어요.")
        );
        notificationRepository.saveAndFlush(
                Notification.create(me.getId(), NotificationType.MESSAGE_RECEIVED, conversationId, sourceMessageId, "새 메시지가 도착했어요.")
        );

        mockMvc(restDocumentation)
                .perform(get("/api/home/reminder-summary")
                        .header(HttpHeaders.AUTHORIZATION, DocsTestSupport.auth(token)))
                .andExpect(status().isOk())
                .andDo(document("home-reminder-summary-get",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(
                                headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer {accessToken}")
                        ),
                        responseFields(
                                fieldWithPath("summary.unreadCount").type(NUMBER).description("전체 미확인 알림 수"),
                                fieldWithPath("summary.unreadReminderCount").type(NUMBER).description("읽지 않은 리마인더 수"),
                                fieldWithPath("summary.todayReminderCount").type(NUMBER).description("오늘 생성된 리마인더 알림 수"),
                                fieldWithPath("lead.id").type(STRING).description("대표 알림 ID(UUID)"),
                                fieldWithPath("lead.type").type(STRING).description("대표 알림 타입"),
                                fieldWithPath("lead.refId").type(STRING).description("대표 알림 refId"),
                                fieldWithPath("lead.ref2Id").type(STRING).optional().description("대표 알림 ref2Id"),
                                fieldWithPath("lead.conversationId").type(STRING).optional().description("대표 알림과 연결된 대화방 ID"),
                                fieldWithPath("lead.body").type(STRING).description("대표 알림 본문"),
                                fieldWithPath("lead.read").type(BOOLEAN).description("대표 알림 읽음 여부"),
                                fieldWithPath("lead.createdAt").type(STRING).description("대표 알림 생성 시각")
                        )
                ));
    }
}
