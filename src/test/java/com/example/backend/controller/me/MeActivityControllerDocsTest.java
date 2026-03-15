package com.example.backend.controller.me;

import com.example.backend.controller.DocsTestSupport;
import com.example.backend.domain.message.MessageCapsule;
import com.example.backend.domain.pin.ConversationPin;
import com.example.backend.domain.post.Post;
import com.example.backend.domain.post.PostVisibility;
import com.example.backend.repository.message.MessageCapsuleRepository;
import com.example.backend.repository.pin.ConversationPinRepository;
import com.example.backend.repository.post.PostRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
import static org.springframework.restdocs.payload.JsonFieldType.ARRAY;
import static org.springframework.restdocs.payload.JsonFieldType.BOOLEAN;
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
class MeActivityControllerDocsTest {

    @Autowired WebApplicationContext context;
    @Autowired DocsTestSupport docs;
    @Autowired ConversationPinRepository pinRepository;
    @Autowired MessageCapsuleRepository capsuleRepository;
    @Autowired PostRepository postRepository;

    private MockMvc mockMvc(RestDocumentationContextProvider restDocumentation) {
        return MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .apply(documentationConfiguration(restDocumentation))
                .build();
    }

    @Test
    void 내_액션_목록_조회_200(RestDocumentationContextProvider restDocumentation) throws Exception {
        MockMvc mockMvc = mockMvc(restDocumentation);
        var me = docs.saveUser("my-action", "나");
        String token = docs.issueTokenFor(me);

        UUID conversationId = UUID.randomUUID();
        UUID sourceMessageId = UUID.randomUUID();
        pinRepository.saveAndFlush(ConversationPin.createSchedule(
                conversationId, me.getId(), sourceMessageId, "모란 먹켓치킨", "모란 먹켓치킨",
                LocalDateTime.now().plusDays(1).withHour(23).withMinute(0).withSecond(0).withNano(0)
        ));

        mockMvc.perform(get("/api/me/activity/actions")
                        .header(DocsTestSupport.headerName(), DocsTestSupport.auth(token)))
                .andExpect(status().isOk())
                .andDo(document("me-activity-actions-get",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(headerWithName(DocsTestSupport.headerName()).description("Bearer {accessToken}")),
                        responseFields(
                                fieldWithPath("items").type(ARRAY).description("내 액션 목록"),
                                fieldWithPath("items[].pinId").type(STRING).description("핀 ID(UUID)"),
                                fieldWithPath("items[].conversationId").type(STRING).description("원본 대화방 ID(UUID)"),
                                fieldWithPath("items[].sourceMessageId").type(STRING).optional().description("원본 메시지 ID(UUID)"),
                                fieldWithPath("items[].type").type(STRING).description("액션 타입"),
                                fieldWithPath("items[].title").type(STRING).description("액션 제목"),
                                fieldWithPath("items[].placeText").type(STRING).optional().description("장소 텍스트"),
                                fieldWithPath("items[].startAt").type(STRING).optional().description("시작 시각"),
                                fieldWithPath("items[].status").type(STRING).description("상태")
                        )
                ));
    }

    @Test
    void 내_캡슐_목록_조회_200(RestDocumentationContextProvider restDocumentation) throws Exception {
        MockMvc mockMvc = mockMvc(restDocumentation);
        var me = docs.saveUser("my-capsule", "나");
        String token = docs.issueTokenFor(me);

        UUID conversationId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();
        capsuleRepository.saveAndFlush(MessageCapsule.create(
                messageId, conversationId, me.getId(), "1년뒤 열어봐라", LocalDateTime.now().plusDays(30)
        ));

        mockMvc.perform(get("/api/me/activity/capsules")
                        .header(DocsTestSupport.headerName(), DocsTestSupport.auth(token)))
                .andExpect(status().isOk())
                .andDo(document("me-activity-capsules-get",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(headerWithName(DocsTestSupport.headerName()).description("Bearer {accessToken}")),
                        responseFields(
                                fieldWithPath("items").type(ARRAY).description("내 캡슐 목록"),
                                fieldWithPath("items[].capsuleId").type(STRING).description("캡슐 ID(UUID)"),
                                fieldWithPath("items[].conversationId").type(STRING).description("원본 대화방 ID(UUID)"),
                                fieldWithPath("items[].messageId").type(STRING).description("원본 메시지 ID(UUID)"),
                                fieldWithPath("items[].title").type(STRING).description("캡슐 제목"),
                                fieldWithPath("items[].unlockAt").type(STRING).description("열릴 시각"),
                                fieldWithPath("items[].opened").type(BOOLEAN).description("열림 여부")
                        )
                ));
    }

    @Test
    void 내_공유_목록_조회_200(RestDocumentationContextProvider restDocumentation) throws Exception {
        MockMvc mockMvc = mockMvc(restDocumentation);
        var me = docs.saveUser("my-share", "나");
        String token = docs.issueTokenFor(me);

        postRepository.saveAndFlush(Post.create(me.getId(), "오늘 액션 공유", PostVisibility.ALL));

        mockMvc.perform(get("/api/me/activity/shares")
                        .header(DocsTestSupport.headerName(), DocsTestSupport.auth(token)))
                .andExpect(status().isOk())
                .andDo(document("me-activity-shares-get",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(headerWithName(DocsTestSupport.headerName()).description("Bearer {accessToken}")),
                        responseFields(
                                fieldWithPath("items").type(ARRAY).description("내 공유 목록"),
                                fieldWithPath("items[].postId").type(STRING).description("게시글 ID(UUID)"),
                                fieldWithPath("items[].content").type(STRING).description("게시글 본문"),
                                fieldWithPath("items[].visibility").type(STRING).description("공개 범위"),
                                fieldWithPath("items[].createdAt").type(STRING).description("작성 시각")
                        )
                ));
    }
}
