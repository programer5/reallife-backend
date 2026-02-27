package com.example.backend.controller.message;

import com.example.backend.controller.DocsTestSupport;
import com.example.backend.domain.message.Conversation;
import com.example.backend.domain.message.ConversationMember;
import com.example.backend.domain.pin.ConversationPin;
import com.example.backend.repository.message.ConversationMemberRepository;
import com.example.backend.repository.message.ConversationRepository;
import com.example.backend.repository.pin.ConversationPinRepository;
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

import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.JsonFieldType.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.relaxedResponseFields;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@ExtendWith({RestDocumentationExtension.class, SpringExtension.class})
@Transactional
class ConversationPinControllerDocsTest {

    @Autowired private WebApplicationContext context;
    @Autowired private DocsTestSupport docs;

    @Autowired private ConversationRepository conversationRepository;
    @Autowired private ConversationMemberRepository memberRepository;
    @Autowired private ConversationPinRepository pinRepository;

    private MockMvc mockMvc(RestDocumentationContextProvider restDocumentation) {
        return MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .apply(documentationConfiguration(restDocumentation))
                .build();
    }

    @Test
    void 대화핀_ACTIVE_목록조회_200(RestDocumentationContextProvider restDocumentation) throws Exception {
        MockMvc mockMvc = mockMvc(restDocumentation);

        var me = docs.saveUser("pin", "나");
        var peer = docs.saveUser("pin2", "상대");
        String token = docs.issueTokenFor(me);

        Conversation c = conversationRepository.save(Conversation.direct());
        memberRepository.save(ConversationMember.join(c.getId(), me.getId()));
        memberRepository.save(ConversationMember.join(c.getId(), peer.getId()));

        // ✅ 핀 1개 생성(파서 의존 없이 레포로 직접 생성)
        // createSchedule 시그니처 변경됨: (conversationId, createdBy, sourceMessageId, title, placeText, startAt)
        pinRepository.save(ConversationPin.createSchedule(
                c.getId(),
                me.getId(),
                null, // ✅ sourceMessageId (DocsTest에서는 없어도 됨)
                "약속",
                "홍대",
                LocalDateTime.now()
                        .plusDays(1)
                        .withHour(19).withMinute(0).withSecond(0).withNano(0)
        ));

        mockMvc.perform(get("/api/conversations/{conversationId}/pins", c.getId())
                        .param("size", "10")
                        .header(DocsTestSupport.headerName(), DocsTestSupport.auth(token))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(document("conversation-pins-get",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(
                                headerWithName(DocsTestSupport.headerName()).description("Bearer {accessToken}")
                        ),
                        pathParameters(
                                parameterWithName("conversationId").description("대화방 ID(UUID)")
                        ),
                        queryParameters(
                                parameterWithName("size").optional().description("조회 개수(default 10, max 50)")
                        ),
                        relaxedResponseFields(
                                fieldWithPath("[]").type(ARRAY).description("ACTIVE 핀 목록(최신순)"),
                                fieldWithPath("[].pinId").type(STRING).description("핀 ID(UUID)"),
                                fieldWithPath("[].conversationId").type(STRING).description("대화방 ID(UUID)"),
                                fieldWithPath("[].createdBy").type(STRING).description("생성자 userId(UUID)"),
                                fieldWithPath("[].type").type(STRING).description("핀 타입(SCHEDULE)"),
                                fieldWithPath("[].title").type(STRING).description("핀 제목(예: 약속)"),
                                fieldWithPath("[].placeText").type(STRING).optional().description("장소 텍스트(예: 홍대)"),
                                fieldWithPath("[].startAt").type(STRING).optional().description("시작 시각(ISO-8601)"),
                                fieldWithPath("[].remindAt").type(STRING).optional().description("리마인드 시각(ISO-8601)"),
                                fieldWithPath("[].status").type(STRING).description("상태(ACTIVE|DONE|CANCELED)"),
                                fieldWithPath("[].createdAt").type(STRING).description("생성 시각(ISO-8601)")
                        )
                ));
    }
}