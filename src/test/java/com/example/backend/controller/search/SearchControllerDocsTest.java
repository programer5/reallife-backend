package com.example.backend.controller.search;

import com.example.backend.controller.DocsTestSupport;
import com.example.backend.domain.message.Conversation;
import com.example.backend.domain.message.ConversationMember;
import com.example.backend.domain.message.Message;
import com.example.backend.domain.message.MessageCapsule;
import com.example.backend.domain.pin.ConversationPin;
import com.example.backend.repository.message.ConversationMemberRepository;
import com.example.backend.repository.message.ConversationRepository;
import com.example.backend.repository.message.MessageCapsuleRepository;
import com.example.backend.repository.message.MessageRepository;
import com.example.backend.repository.pin.ConversationPinRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.payload.JsonFieldType.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@ExtendWith({RestDocumentationExtension.class, SpringExtension.class})
@Transactional
class SearchControllerDocsTest {

    @Autowired WebApplicationContext context;
    @Autowired DocsTestSupport docs;
    @Autowired ConversationRepository conversationRepository;
    @Autowired ConversationMemberRepository conversationMemberRepository;
    @Autowired MessageRepository messageRepository;
    @Autowired MessageCapsuleRepository messageCapsuleRepository;
    @Autowired ConversationPinRepository conversationPinRepository;

    private MockMvc mockMvc(RestDocumentationContextProvider restDocumentation) {
        return MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .apply(documentationConfiguration(restDocumentation))
                .build();
    }

    @Test
    void 통합검색_200(RestDocumentationContextProvider restDocumentation) throws Exception {
        var me = docs.saveUser("search-api", "검색유저");
        String token = docs.issueTokenFor(me);

        var conversation = conversationRepository.saveAndFlush(Conversation.direct());
        conversationMemberRepository.saveAndFlush(ConversationMember.join(conversation.getId(), me.getId()));

        var message = messageRepository.saveAndFlush(Message.text(conversation.getId(), me.getId(), "이번 주말 영화 약속 다시 정리하자"));
        var pin = conversationPinRepository.saveAndFlush(
                ConversationPin.createSchedule(conversation.getId(), me.getId(), message.getId(), "영화 약속 잡기", "강남", LocalDateTime.now().plusDays(2), 30)
        );
        var capsule = messageCapsuleRepository.saveAndFlush(
                MessageCapsule.create(message.getId(), conversation.getId(), me.getId(), "영화 보고 열어보기", LocalDateTime.now().plusDays(7))
        );
        docs.savePost(me.getId(), "영화 후기와 다음 약속 아이디어를 피드에 남겼어요.");

        mockMvc(restDocumentation)
                .perform(get("/api/search")
                        .queryParam("q", "영화")
                        .queryParam("conversationId", conversation.getId().toString())
                        .queryParam("type", "ALL")
                        .queryParam("limit", "6")
                        .header(HttpHeaders.AUTHORIZATION, DocsTestSupport.auth(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.query").value("영화"))
                .andExpect(jsonPath("$.sections").isArray())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items[0].deepLink").exists())
                .andDo(document("search-unified-200",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(
                                headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer {accessToken}")
                        ),
                        queryParameters(
                                parameterWithName("q").description("검색어"),
                                parameterWithName("type").optional().description("검색 타입(ALL, MESSAGES, ACTIONS, CAPSULES, POSTS)"),
                                parameterWithName("conversationId").optional().description("대화방 범위 검색 시 conversationId"),
                                parameterWithName("limit").optional().description("섹션별 조회 개수(기본 6, 최대 20)")
                        ),
                        responseFields(
                                fieldWithPath("query").type(STRING).description("실행된 검색어"),
                                fieldWithPath("type").type(STRING).description("적용된 검색 타입"),
                                fieldWithPath("conversationId").optional().type(STRING).description("대화방 범위 검색에 사용된 conversationId"),
                                fieldWithPath("sections").type(ARRAY).description("검색 결과 섹션 요약"),
                                fieldWithPath("sections[].type").type(STRING).description("섹션 타입"),
                                fieldWithPath("sections[].label").type(STRING).description("섹션 라벨"),
                                fieldWithPath("sections[].count").type(NUMBER).description("섹션별 결과 수"),
                                fieldWithPath("items").type(ARRAY).description("통합 검색 결과 목록"),
                                fieldWithPath("items[].type").type(STRING).description("결과 타입(MESSAGES, ACTIONS, CAPSULES, POSTS)"),
                                fieldWithPath("items[].id").type(STRING).description("결과 엔티티 ID"),
                                fieldWithPath("items[].title").type(STRING).description("결과 제목"),
                                fieldWithPath("items[].snippet").type(STRING).description("짧은 미리보기 텍스트"),
                                fieldWithPath("items[].highlight").type(STRING).description("검색어가 포착된 강조 텍스트"),
                                fieldWithPath("items[].createdAt").type(STRING).description("생성 시각"),
                                fieldWithPath("items[].conversationId").optional().type(STRING).description("연결된 conversationId"),
                                fieldWithPath("items[].deepLink").type(STRING).description("프론트 deep link"),
                                fieldWithPath("items[].badge").type(STRING).description("타입/상태 badge"),
                                fieldWithPath("items[].secondary").type(STRING).description("보조 설명"),
                                fieldWithPath("items[].anchorType").type(STRING).description("재진입 anchor 타입(MESSAGE, PIN, CAPSULE, POST)"),
                                fieldWithPath("items[].anchorId").type(STRING).description("재진입 anchor ID"),
                                fieldWithPath("items[].relevance").type(NUMBER).description("관련도 점수(클수록 상단 노출)"),
                                fieldWithPath("meta").type(OBJECT).description("검색 백엔드 메타 정보"),
                                fieldWithPath("meta.elasticReady").type(BOOLEAN).description("Elasticsearch 인덱서 준비 여부"),
                                fieldWithPath("meta.backend").type(STRING).description("현재 사용 중인 검색 백엔드 모드"),
                                fieldWithPath("meta.requestedLimit").type(NUMBER).description("요청 limit"),
                                fieldWithPath("meta.availableTypes").type(ARRAY).description("지원 검색 타입 목록")
                        )
                ));
    }
}
