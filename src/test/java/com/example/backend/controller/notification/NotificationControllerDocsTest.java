package com.example.backend.controller.notification;

import com.example.backend.controller.DocsTestSupport;
import com.example.backend.domain.notification.Notification;
import com.example.backend.domain.notification.NotificationType;
import com.example.backend.repository.notification.NotificationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.test.context.ActiveProfiles;
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
import static org.springframework.restdocs.payload.JsonFieldType.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@ExtendWith({RestDocumentationExtension.class, SpringExtension.class})
@Transactional
class NotificationControllerDocsTest {

    @Autowired WebApplicationContext context;
    @Autowired DocsTestSupport docs;
    @Autowired NotificationRepository notificationRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private MockMvc mockMvc(RestDocumentationContextProvider restDocumentation) {
        return MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .apply(documentationConfiguration(restDocumentation))
                .build();
    }

    @Test
    void 알림목록_조회_첫페이지_200(RestDocumentationContextProvider restDocumentation) throws Exception {
        var me = docs.saveUser("notiSelect", "me");
        String token = docs.issueTokenFor(me);

        // size=2 → hasNext=true 되도록 3개 생성
        notificationRepository.save(Notification.create(me.getId(), NotificationType.MESSAGE_RECEIVED, UUID.randomUUID(), "알림 1"));
        notificationRepository.save(Notification.create(me.getId(), NotificationType.MESSAGE_RECEIVED, UUID.randomUUID(), "알림 2"));
        notificationRepository.save(Notification.create(me.getId(), NotificationType.MESSAGE_RECEIVED, UUID.randomUUID(), "알림 3"));
        notificationRepository.flush();

        mockMvc(restDocumentation)
                .perform(get("/api/notifications")
                        .param("size", "2")
                        .header(HttpHeaders.AUTHORIZATION, DocsTestSupport.auth(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.hasNext").value(true))
                .andExpect(jsonPath("$.items[0].read").value(false))
                .andDo(document("notifications-get-200",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(
                                headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer {accessToken}")
                        ),
                        queryParameters(
                                parameterWithName("cursor").optional().description("페이지 커서(createdAt|id). 없으면 첫 페이지"),
                                parameterWithName("size").optional().description("페이지 크기(기본 20, 최대 50)")
                        ),
                        responseFields(
                                fieldWithPath("items").type(ARRAY).description("알림 목록(최신순)"),
                                fieldWithPath("items[].id").type(STRING).description("알림 ID(UUID)"),
                                fieldWithPath("items[].type").type(STRING).description("알림 타입"),
                                fieldWithPath("items[].body").type(STRING).description("알림 내용"),
                                fieldWithPath("items[].read").type(BOOLEAN).description("읽음 여부"),
                                fieldWithPath("items[].createdAt").type(STRING).description("생성 시각"),
                                fieldWithPath("nextCursor").optional().type(STRING).description("다음 페이지 커서(없으면 null)"),
                                fieldWithPath("hasNext").type(BOOLEAN).description("다음 페이지 존재 여부"),
                                fieldWithPath("hasUnread").type(BOOLEAN).description("미읽음 알림 존재 여부")
                        )
                ));
    }

    @Test
    void 알림목록_조회_다음페이지_200(RestDocumentationContextProvider restDocumentation) throws Exception {
        var me = docs.saveUser("notiNext", "me");
        String token = docs.issueTokenFor(me);

        notificationRepository.save(Notification.create(me.getId(), NotificationType.MESSAGE_RECEIVED, UUID.randomUUID(), "알림 1"));
        notificationRepository.save(Notification.create(me.getId(), NotificationType.MESSAGE_RECEIVED, UUID.randomUUID(), "알림 2"));
        notificationRepository.save(Notification.create(me.getId(), NotificationType.MESSAGE_RECEIVED, UUID.randomUUID(), "알림 3"));
        notificationRepository.save(Notification.create(me.getId(), NotificationType.MESSAGE_RECEIVED, UUID.randomUUID(), "알림 4"));
        notificationRepository.save(Notification.create(me.getId(), NotificationType.MESSAGE_RECEIVED, UUID.randomUUID(), "알림 5"));
        notificationRepository.flush();

        String firstPage = mockMvc(restDocumentation)
                .perform(get("/api/notifications")
                        .param("size", "2")
                        .header(HttpHeaders.AUTHORIZATION, DocsTestSupport.auth(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nextCursor").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String nextCursor = objectMapper.readTree(firstPage).path("nextCursor").asText();

        mockMvc(restDocumentation)
                .perform(get("/api/notifications")
                        .param("size", "2")
                        .param("cursor", nextCursor)
                        .header(HttpHeaders.AUTHORIZATION, DocsTestSupport.auth(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").exists())
                .andDo(document("notifications-get-200-next-page",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(
                                headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer {accessToken}")
                        ),
                        queryParameters(
                                parameterWithName("cursor").optional().description("페이지 커서(createdAt|id)"),
                                parameterWithName("size").optional().description("페이지 크기(기본 20, 최대 50)")
                        ),
                        responseFields(
                                fieldWithPath("items").type(ARRAY).description("알림 목록(최신순)"),
                                fieldWithPath("items[].id").type(STRING).description("알림 ID(UUID)"),
                                fieldWithPath("items[].type").type(STRING).description("알림 타입"),
                                fieldWithPath("items[].body").type(STRING).description("알림 내용"),
                                fieldWithPath("items[].read").type(BOOLEAN).description("읽음 여부"),
                                fieldWithPath("items[].createdAt").type(STRING).description("생성 시각"),
                                fieldWithPath("nextCursor").optional().type(STRING).description("다음 페이지 커서(없으면 null)"),
                                fieldWithPath("hasNext").type(BOOLEAN).description("다음 페이지 존재 여부"),
                                fieldWithPath("hasUnread").type(BOOLEAN).description("미읽음 알림 존재 여부")
                        )
                ));
    }

    @Test
    void 알림_단건_읽음처리_200(RestDocumentationContextProvider restDocumentation) throws Exception {
        var me = docs.saveUser("notiRead", "me");
        String token = docs.issueTokenFor(me);

        Notification n = notificationRepository.save(
                Notification.create(me.getId(), NotificationType.MESSAGE_RECEIVED, UUID.randomUUID(), "단건 읽음 테스트")
        );
        notificationRepository.flush();

        mockMvc(restDocumentation)
                .perform(post("/api/notifications/{id}/read", n.getId())
                        .header(HttpHeaders.AUTHORIZATION, DocsTestSupport.auth(token))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(document("notifications-read-200",
                        preprocessRequest(prettyPrint()),
                        requestHeaders(
                                headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer {accessToken}")
                        ),
                        pathParameters(
                                parameterWithName("id").description("알림 ID(UUID)")
                        )
                ));
    }

    @Test
    void 알림_전체_읽음처리_200(RestDocumentationContextProvider restDocumentation) throws Exception {
        var me = docs.saveUser("notiAllRead", "me");
        String token = docs.issueTokenFor(me);

        notificationRepository.save(Notification.create(me.getId(), NotificationType.MESSAGE_RECEIVED, UUID.randomUUID(), "알림 1"));
        notificationRepository.save(Notification.create(me.getId(), NotificationType.MESSAGE_RECEIVED, UUID.randomUUID(), "알림 2"));
        notificationRepository.flush();

        mockMvc(restDocumentation)
                .perform(post("/api/notifications/read-all")
                        .header(HttpHeaders.AUTHORIZATION, DocsTestSupport.auth(token))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(document("notifications-read-all-200",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(
                                headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer {accessToken}")
                        ),
                        responseFields(
                                fieldWithPath("updatedCount").type(NUMBER)
                                        .description("이번 요청으로 읽음 처리된 알림 개수")
                        )
                ));
    }

    @Test
    void 읽은알림_일괄삭제_softdelete_200(RestDocumentationContextProvider restDocumentation) throws Exception {
        var me = docs.saveUser("notiAllDelete", "me");
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
                                fieldWithPath("deletedCount").type(NUMBER)
                                        .description("이번 요청으로 삭제 처리된(soft delete) 알림 개수")
                        )
                ));
    }
}