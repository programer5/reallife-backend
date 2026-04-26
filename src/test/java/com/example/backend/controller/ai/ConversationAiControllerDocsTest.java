package com.example.backend.controller.ai;

import com.example.backend.controller.DocsTestSupport;
import com.example.backend.domain.message.Conversation;
import com.example.backend.domain.message.ConversationMember;
import com.example.backend.repository.message.ConversationMemberRepository;
import com.example.backend.repository.message.ConversationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
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
import static org.springframework.restdocs.payload.JsonFieldType.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.relaxedRequestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.relaxedResponseFields;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@ExtendWith({RestDocumentationExtension.class, SpringExtension.class})
@Transactional
class ConversationAiControllerDocsTest {

    @Autowired private WebApplicationContext context;
    @Autowired private DocsTestSupport docs;
    @Autowired private ConversationRepository conversationRepository;
    @Autowired private ConversationMemberRepository memberRepository;

    private MockMvc mockMvc(RestDocumentationContextProvider restDocumentation) {
        return MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .apply(documentationConfiguration(restDocumentation))
                .build();
    }

    private Conversation seedConversation(UUID meId) {
        var peer = docs.saveUser("ai_peer", "상대");
        Conversation conversation = conversationRepository.saveAndFlush(Conversation.direct());
        memberRepository.saveAndFlush(ConversationMember.join(conversation.getId(), meId));
        memberRepository.saveAndFlush(ConversationMember.join(conversation.getId(), peer.getId()));
        return conversation;
    }

    @Test
    void AI_추천답장_및_액션후보_생성_200(RestDocumentationContextProvider restDocumentation) throws Exception {
        MockMvc mockMvc = mockMvc(restDocumentation);

        var me = docs.saveUser("aiReply", "나");
        String token = docs.issueTokenFor(me);
        Conversation conversation = seedConversation(me.getId());

        String body = """
                {
                  "conversationId": "%s",
                  "messageId": null,
                  "text": "내일 오후 7시에 모란 카페에서 만나자"
                }
                """.formatted(conversation.getId());

        mockMvc.perform(post("/api/ai/reply")
                        .header(DocsTestSupport.headerName(), DocsTestSupport.auth(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andDo(document("conversation-ai-reply",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(
                                headerWithName(DocsTestSupport.headerName()).description("Bearer {accessToken}")
                        ),
                        relaxedRequestFields(
                                fieldWithPath("conversationId").type(STRING).description("대화방 ID(UUID)"),
                                fieldWithPath("messageId").type(NULL).optional().description("기준 메시지 ID(UUID). 없으면 null"),
                                fieldWithPath("text").type(STRING).description("AI가 분석할 최근 메시지 또는 사용자가 선택한 텍스트")
                        ),
                        relaxedResponseFields(
                                fieldWithPath("replies").type(ARRAY).description("추천 답장 목록(최대 3개). 각 항목은 추천 답장 문자열"),
                                fieldWithPath("actions").type(ARRAY).description("추천 액션 목록(최대 3개)"),
                                fieldWithPath("actions[].type").type(STRING).description("액션 타입(schedule|reminder|map|notify|focus)"),
                                fieldWithPath("actions[].label").type(STRING).description("프론트 버튼에 표시할 짧은 라벨"),
                                fieldWithPath("actions[].payload").type(OBJECT).description("액션 실행 시 다시 전달할 보조 데이터"),
                                fieldWithPath("source").type(STRING).description("추천 출처(openai|rule|rule-fallback|rule-empty)")
                        )
                ));
    }

    @Test
    void AI_액션실행_일정핀생성_200(RestDocumentationContextProvider restDocumentation) throws Exception {
        MockMvc mockMvc = mockMvc(restDocumentation);

        var me = docs.saveUser("aiAction", "나");
        String token = docs.issueTokenFor(me);
        Conversation conversation = seedConversation(me.getId());
        String startAt = LocalDateTime.now()
                .plusDays(1)
                .withHour(19)
                .withMinute(0)
                .withSecond(0)
                .withNano(0)
                .toString();

        String body = """
                {
                  "conversationId": "%s",
                  "messageId": null,
                  "type": "schedule",
                  "text": "내일 오후 7시에 모란 카페에서 만나자",
                  "payload": {
                    "title": "약속",
                    "placeText": "모란 카페",
                    "startAt": "%s",
                    "remindMinutes": 30
                  }
                }
                """.formatted(conversation.getId(), startAt);

        mockMvc.perform(post("/api/ai/actions/execute")
                        .header(DocsTestSupport.headerName(), DocsTestSupport.auth(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andDo(document("conversation-ai-action-execute",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(
                                headerWithName(DocsTestSupport.headerName()).description("Bearer {accessToken}")
                        ),
                        relaxedRequestFields(
                                fieldWithPath("conversationId").type(STRING).description("대화방 ID(UUID)"),
                                fieldWithPath("messageId").type(NULL).optional().description("기준 메시지 ID(UUID). 없으면 null"),
                                fieldWithPath("type").type(STRING).description("실행할 액션 타입(schedule|reminder|map|notify|focus)"),
                                fieldWithPath("text").type(STRING).description("액션 생성의 원본 메시지/문장"),
                                fieldWithPath("payload").type(OBJECT).description("액션별 보조 데이터"),
                                fieldWithPath("payload.title").type(STRING).optional().description("일정/알림 제목"),
                                fieldWithPath("payload.placeText").type(STRING).optional().description("장소 텍스트"),
                                fieldWithPath("payload.startAt").type(STRING).optional().description("시작 시각(ISO-8601)"),
                                fieldWithPath("payload.remindMinutes").type(NUMBER).optional().description("시작 전 리마인드 분(0|5|10|30|60)")
                        ),
                        relaxedResponseFields(
                                fieldWithPath("status").type(STRING).description("처리 결과(ok)"),
                                fieldWithPath("type").type(STRING).description("실행된 액션 타입"),
                                fieldWithPath("message").type(STRING).description("사용자에게 보여줄 처리 결과 메시지"),
                                fieldWithPath("targetUrl").type(STRING).optional().description("처리 후 이동 가능한 프론트 경로"),
                                fieldWithPath("payload").type(OBJECT).description("실행 결과 보조 데이터"),
                                fieldWithPath("payload.pinId").type(STRING).optional().description("일정/알림 액션으로 생성된 핀 ID(UUID)"),
                                fieldWithPath("payload.startAt").type(STRING).optional().description("생성된 핀 시작 시각"),
                                fieldWithPath("payload.remindAt").type(STRING).optional().description("생성된 핀 리마인드 시각")
                        )
                ));
    }
}
