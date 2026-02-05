package com.example.backend.controller.notification;

import com.example.backend.controller.DocsTestSupport;
import com.example.backend.domain.notification.Notification;
import com.example.backend.domain.notification.NotificationType;
import com.example.backend.restdocs.ErrorResponseSnippet;
import com.example.backend.repository.notification.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.UUID;

import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ExtendWith({RestDocumentationExtension.class, SpringExtension.class})
@Transactional
class NotificationControllerDocsTest {

    @Autowired WebApplicationContext context;
    @Autowired DocsTestSupport docs;
    @Autowired NotificationRepository notificationRepository;

    private MockMvc mockMvc(RestDocumentationContextProvider restDocumentation) {
        return MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .apply(documentationConfiguration(restDocumentation))
                .build();
    }

    @Test
    void 알림_전체읽음_성공_200(RestDocumentationContextProvider restDocumentation) throws Exception {
        var me = docs.saveUser("noti", "me");
        String token = docs.issueTokenFor(me);

        notificationRepository.save(Notification.create(me.getId(), NotificationType.MESSAGE_RECEIVED, UUID.randomUUID(), "새 메시지 1"));
        notificationRepository.save(Notification.create(me.getId(), NotificationType.MESSAGE_RECEIVED, UUID.randomUUID(), "새 메시지 2"));
        notificationRepository.flush();

        mockMvc(restDocumentation)
                .perform(post("/api/notifications/read-all")
                        .header(HttpHeaders.AUTHORIZATION, DocsTestSupport.auth(token)))
                .andExpect(status().isOk())
                .andDo(document("notifications-read-all-200",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(
                                headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer {accessToken}")
                        ),
                        responseFields(
                                fieldWithPath("updatedCount").description("이번 요청으로 읽음 처리된 알림 개수")
                        )
                ));
    }

    @Test
    void 읽은알림_일괄삭제_성공_200(RestDocumentationContextProvider restDocumentation) throws Exception {
        var me = docs.saveUser("noti", "me");
        String token = docs.issueTokenFor(me);

        Notification read1 = Notification.create(me.getId(), NotificationType.MESSAGE_RECEIVED, UUID.randomUUID(), "읽은 알림 1");
        Notification read2 = Notification.create(me.getId(), NotificationType.MESSAGE_RECEIVED, UUID.randomUUID(), "읽은 알림 2");
        Notification unread = Notification.create(me.getId(), NotificationType.MESSAGE_RECEIVED, UUID.randomUUID(), "미읽음 알림");

        read1.markAsRead();
        read2.markAsRead();

        notificationRepository.save(read1);
        notificationRepository.save(read2);
        notificationRepository.save(unread);
        notificationRepository.flush();

        mockMvc(restDocumentation)
                .perform(delete("/api/notifications/read")
                        .header(HttpHeaders.AUTHORIZATION, DocsTestSupport.auth(token)))
                .andExpect(status().isOk())
                .andDo(document("notifications-delete-read-200",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(
                                headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer {accessToken}")
                        ),
                        responseFields(
                                fieldWithPath("deletedCount").description("이번 요청으로 삭제 처리된(soft delete) 알림 개수")
                        )
                ));
    }

    @Test
    void 알림_단건읽음_성공_200(RestDocumentationContextProvider restDocumentation) throws Exception {
        var me = docs.saveUser("noti", "me");
        String token = docs.issueTokenFor(me);

        Notification n = notificationRepository.save(
                Notification.create(me.getId(), NotificationType.MESSAGE_RECEIVED, UUID.randomUUID(), "단건 읽음 테스트")
        );
        notificationRepository.flush();

        mockMvc(restDocumentation)
                .perform(post("/api/notifications/{id}/read", n.getId())
                        .header(HttpHeaders.AUTHORIZATION, DocsTestSupport.auth(token)))
                .andExpect(status().isOk())
                .andDo(document("notifications-read-200",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(
                                headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer {accessToken}")
                        ),
                        pathParameters(
                                parameterWithName("id").description("알림 ID(UUID)")
                        )
                ));
    }

    @Test
    void 알림_단건읽음_실패_404(RestDocumentationContextProvider restDocumentation) throws Exception {
        var me = docs.saveUser("noti", "me");
        String token = docs.issueTokenFor(me);

        UUID notFoundId = UUID.randomUUID();

        mockMvc(restDocumentation)
                .perform(post("/api/notifications/{id}/read", notFoundId)
                        .header(HttpHeaders.AUTHORIZATION, DocsTestSupport.auth(token)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOTIFICATION_NOT_FOUND"))
                .andDo(document("notifications-read-404-not-found",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(
                                headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer {accessToken}")
                        ),
                        pathParameters(
                                parameterWithName("id").description("알림 ID(UUID)")
                        ),
                        responseFields(ErrorResponseSnippet.common())
                ));
    }

    @Test
    void 알림목록_조회_성공_200(RestDocumentationContextProvider restDocumentation) throws Exception {
        var me = docs.saveUser("noti", "me");
        String token = docs.issueTokenFor(me);

        Notification n1 = Notification.create(me.getId(), NotificationType.MESSAGE_RECEIVED, UUID.randomUUID(), "알림 1");
        Notification n2 = Notification.create(me.getId(), NotificationType.MESSAGE_RECEIVED, UUID.randomUUID(), "알림 2");
        n2.markAsRead();

        notificationRepository.save(n1);
        notificationRepository.save(n2);
        notificationRepository.flush();

        mockMvc(restDocumentation)
                .perform(get("/api/notifications")
                        .header(HttpHeaders.AUTHORIZATION, DocsTestSupport.auth(token)))
                .andExpect(status().isOk())
                .andDo(document("notifications-get-200",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(
                                headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer {accessToken}")
                        ),
                        responseFields(
                                fieldWithPath("items").description("알림 목록(최신순)"),
                                fieldWithPath("items[].id").description("알림 ID(UUID)"),
                                fieldWithPath("items[].type").description("알림 타입"),
                                fieldWithPath("items[].body").description("알림 내용"),
                                fieldWithPath("items[].read").description("읽음 여부"),
                                fieldWithPath("items[].createdAt").description("생성 시각"),
                                fieldWithPath("hasUnread").description("미읽음 알림 존재 여부")
                        )
                ));
    }
}