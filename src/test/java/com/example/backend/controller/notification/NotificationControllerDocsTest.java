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
import static org.springframework.restdocs.payload.PayloadDocumentation.relaxedResponseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
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
        return MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).apply(documentationConfiguration(restDocumentation)).build();
    }

    @Test
    void 알림목록_조회_첫페이지_200(RestDocumentationContextProvider restDocumentation) throws Exception {
        var me = docs.saveUser("notiSelect", "me");
        String token = docs.issueTokenFor(me);
        Notification reaction = Notification.create(me.getId(), NotificationType.POST_LIKE, UUID.randomUUID(), "좋아요 알림");
        reaction.markAsRead();
        notificationRepository.save(reaction);
        notificationRepository.save(Notification.create(me.getId(), NotificationType.MESSAGE_RECEIVED, UUID.randomUUID(), UUID.randomUUID(), "메시지 알림"));
        notificationRepository.save(Notification.create(me.getId(), NotificationType.PIN_REMIND, UUID.randomUUID(), UUID.randomUUID(), "리마인더 알림"));
        notificationRepository.flush();

        mockMvc(restDocumentation)
                .perform(get("/api/notifications").param("size", "2").header(HttpHeaders.AUTHORIZATION, DocsTestSupport.auth(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items[0].priorityScore").isNumber())
                .andDo(document("notifications-get-200",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer {accessToken}")),
                        queryParameters(
                                parameterWithName("cursor").optional().description("페이지 커서(priorityScore|createdAt). 없으면 첫 페이지"),
                                parameterWithName("size").optional().description("페이지 크기(기본 20, 최대 50)")
                        ),
                        relaxedResponseFields(
                                fieldWithPath("items").type(ARRAY).description("알림 목록(서버 우선순위 + 최신순)"),
                                fieldWithPath("items[].id").type(STRING).description("알림 ID(UUID)"),
                                fieldWithPath("items[].type").type(STRING).description("알림 타입"),
                                fieldWithPath("items[].refId").type(STRING).description("연관 리소스 ID"),
                                fieldWithPath("items[].ref2Id").optional().type(STRING).description("보조 연관 리소스 ID"),
                                fieldWithPath("items[].conversationId").optional().type(STRING).description("대화방 ID"),
                                fieldWithPath("items[].category").type(STRING).description("Inbox 필터용 카테고리(REMINDER/MESSAGE/COMMENT/REACTION/ACTION)"),
                                fieldWithPath("items[].priorityScore").type(NUMBER).description("서버 우선순위 점수(높을수록 먼저 노출)"),
                                fieldWithPath("items[].targetPath").type(STRING).description("원본 화면으로 이동할 경로"),
                                fieldWithPath("items[].targetLabel").type(STRING).description("원본 이동 CTA 라벨"),
                                fieldWithPath("items[].actionHint").type(STRING).description("다음 행동 안내 문구"),
                                fieldWithPath("items[].body").type(STRING).description("알림 내용"),
                                fieldWithPath("items[].read").type(BOOLEAN).description("읽음 여부"),
                                fieldWithPath("items[].createdAt").type(STRING).description("생성 시각"),
                                fieldWithPath("nextCursor").optional().type(STRING).description("다음 페이지 커서(priorityScore|createdAt)"),
                                fieldWithPath("hasNext").type(BOOLEAN).description("다음 페이지 존재 여부"),
                                fieldWithPath("hasUnread").type(BOOLEAN).description("미읽음 알림 존재 여부")
                        )
                ));
    }

    @Test
    void 알림목록_조회_다음페이지_200(RestDocumentationContextProvider restDocumentation) throws Exception {
        var me = docs.saveUser("notiNext", "me");
        String token = docs.issueTokenFor(me);

        notificationRepository.saveAndFlush(Notification.create(me.getId(), NotificationType.PIN_REMIND, UUID.randomUUID(), UUID.randomUUID(), "리마인더 알림"));
        notificationRepository.saveAndFlush(Notification.create(me.getId(), NotificationType.MESSAGE_RECEIVED, UUID.randomUUID(), UUID.randomUUID(), "메시지 알림"));
        for (int i = 0; i < 3; i++) {
            Notification readMessage = Notification.create(me.getId(), NotificationType.MESSAGE_RECEIVED, UUID.randomUUID(), "읽은 알림 " + i);
            readMessage.markAsRead();
            notificationRepository.saveAndFlush(readMessage);
        }

        String firstPage = mockMvc(restDocumentation)
                .perform(get("/api/notifications").param("size", "2").header(HttpHeaders.AUTHORIZATION, DocsTestSupport.auth(token)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String nextCursor = objectMapper.readTree(firstPage).path("nextCursor").asText();
        mockMvc(restDocumentation)
                .perform(get("/api/notifications").param("size", "2").param("cursor", nextCursor).header(HttpHeaders.AUTHORIZATION, DocsTestSupport.auth(token)))
                .andExpect(status().isOk())
                .andDo(document("notifications-get-200-next-page",
                        preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint()),
                        requestHeaders(headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer {accessToken}")),
                        queryParameters(
                                parameterWithName("cursor").optional().description("페이지 커서(priorityScore|createdAt)"),
                                parameterWithName("size").optional().description("페이지 크기(기본 20, 최대 50)")
                        ),
                        relaxedResponseFields(
                                fieldWithPath("items").type(ARRAY).description("알림 목록"),
                                fieldWithPath("items[].id").type(STRING).description("알림 ID"),
                                fieldWithPath("items[].type").type(STRING).description("알림 타입"),
                                fieldWithPath("items[].category").type(STRING).description("알림 카테고리"),
                                fieldWithPath("items[].priorityScore").type(NUMBER).description("서버 우선순위 점수"),
                                fieldWithPath("items[].targetPath").type(STRING).description("원본 이동 경로"),
                                fieldWithPath("items[].body").type(STRING).description("알림 내용"),
                                fieldWithPath("items[].read").type(BOOLEAN).description("읽음 여부"),
                                fieldWithPath("items[].createdAt").type(STRING).description("생성 시각"),
                                fieldWithPath("nextCursor").optional().type(STRING).description("다음 페이지 커서"),
                                fieldWithPath("hasNext").type(BOOLEAN).description("다음 페이지 존재 여부"),
                                fieldWithPath("hasUnread").type(BOOLEAN).description("미읽음 여부")
                        )
                ));
    }

    @Test
    void 알림_단건_읽음처리_200(RestDocumentationContextProvider restDocumentation) throws Exception {
        var me = docs.saveUser("notiRead", "me");
        String token = docs.issueTokenFor(me);
        Notification n = notificationRepository.save(Notification.create(me.getId(), NotificationType.MESSAGE_RECEIVED, UUID.randomUUID(), "단건 읽음 테스트"));
        notificationRepository.flush();
        mockMvc(restDocumentation).perform(post("/api/notifications/{id}/read", n.getId()).header(HttpHeaders.AUTHORIZATION, DocsTestSupport.auth(token)).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(document("notifications-read-200", preprocessRequest(prettyPrint()), requestHeaders(headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer {accessToken}"))));
    }

    @Test
    void 알림_전체_읽음처리_200(RestDocumentationContextProvider restDocumentation) throws Exception {
        var me = docs.saveUser("notiAllRead", "me");
        String token = docs.issueTokenFor(me);
        notificationRepository.save(Notification.create(me.getId(), NotificationType.MESSAGE_RECEIVED, UUID.randomUUID(), "알림 1"));
        notificationRepository.save(Notification.create(me.getId(), NotificationType.MESSAGE_RECEIVED, UUID.randomUUID(), "알림 2"));
        notificationRepository.flush();
        mockMvc(restDocumentation).perform(post("/api/notifications/read-all").header(HttpHeaders.AUTHORIZATION, DocsTestSupport.auth(token)).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(document("notifications-read-all-200", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint()), requestHeaders(headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer {accessToken}")), relaxedResponseFields(fieldWithPath("updatedCount").type(NUMBER).description("이번 요청으로 읽음 처리된 알림 개수"))));
    }

    @Test
    void 읽은알림_일괄삭제_softdelete_200(RestDocumentationContextProvider restDocumentation) throws Exception {
        var me = docs.saveUser("notiAllDelete", "me");
        String token = docs.issueTokenFor(me);
        Notification read1 = Notification.create(me.getId(), NotificationType.MESSAGE_RECEIVED, UUID.randomUUID(), "읽은 알림 1");
        Notification read2 = Notification.create(me.getId(), NotificationType.MESSAGE_RECEIVED, UUID.randomUUID(), "읽은 알림 2");
        Notification unread = Notification.create(me.getId(), NotificationType.MESSAGE_RECEIVED, UUID.randomUUID(), "미읽음 알림");
        read1.markAsRead(); read2.markAsRead();
        notificationRepository.save(read1); notificationRepository.save(read2); notificationRepository.save(unread); notificationRepository.flush();
        mockMvc(restDocumentation).perform(delete("/api/notifications/read").header(HttpHeaders.AUTHORIZATION, DocsTestSupport.auth(token)))
                .andExpect(status().isOk())
                .andDo(document("notifications-delete-read-200", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint()), requestHeaders(headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer {accessToken}")), relaxedResponseFields(fieldWithPath("deletedCount").type(NUMBER).description("이번 요청으로 삭제 처리된 알림 개수"))));
    }
}
