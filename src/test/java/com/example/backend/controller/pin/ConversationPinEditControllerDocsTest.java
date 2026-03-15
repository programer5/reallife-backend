package com.example.backend.controller.pin;

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
import static org.springframework.restdocs.payload.JsonFieldType.NUMBER;
import static org.springframework.restdocs.payload.JsonFieldType.STRING;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@ExtendWith({RestDocumentationExtension.class, SpringExtension.class})
@Transactional
class ConversationPinEditControllerDocsTest {

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

    private ConversationPin preparePin() {
        var me = docs.saveUser("pinEdit", "나");
        var peer = docs.saveUser("pinEdit2", "상대");
        Conversation c = conversationRepository.save(Conversation.direct());
        memberRepository.save(ConversationMember.join(c.getId(), me.getId()));
        memberRepository.save(ConversationMember.join(c.getId(), peer.getId()));
        return pinRepository.save(
                ConversationPin.createSchedule(
                        c.getId(),
                        me.getId(),
                        null,
                        "약속",
                        null,
                        LocalDateTime.now().plusDays(1).withHour(19).withMinute(0).withSecond(0).withNano(0)
                )
        );
    }

    @Test
    void 핀_조회_get_200(RestDocumentationContextProvider restDocumentation) throws Exception {
        MockMvc mockMvc = mockMvc(restDocumentation);
        var me = docs.saveUser("pinGet", "나");
        var peer = docs.saveUser("pinGet2", "상대");
        String token = docs.issueTokenFor(me);
        Conversation c = conversationRepository.save(Conversation.direct());
        memberRepository.save(ConversationMember.join(c.getId(), me.getId()));
        memberRepository.save(ConversationMember.join(c.getId(), peer.getId()));
        ConversationPin pin = pinRepository.save(ConversationPin.createSchedule(c.getId(), me.getId(), null, "약속", "회사 앞", LocalDateTime.now().plusDays(1)));

        mockMvc.perform(get("/api/pins/{pinId}", pin.getId())
                        .header(DocsTestSupport.headerName(), DocsTestSupport.auth(token)))
                .andExpect(status().isOk())
                .andDo(document("pins-get",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(headerWithName(DocsTestSupport.headerName()).description("Bearer {accessToken}")),
                        pathParameters(parameterWithName("pinId").description("핀 ID(UUID)")),
                        responseFields(
                                fieldWithPath("pinId").type(STRING).description("핀 ID(UUID)"),
                                fieldWithPath("conversationId").type(STRING).description("대화방 ID(UUID)"),
                                fieldWithPath("createdBy").type(STRING).description("생성자 ID(UUID)"),
                                fieldWithPath("type").type(STRING).description("핀 타입"),
                                fieldWithPath("title").type(STRING).description("제목"),
                                fieldWithPath("placeText").type(STRING).optional().description("장소 텍스트"),
                                fieldWithPath("startAt").type(STRING).optional().description("시작 시각"),
                                fieldWithPath("remindAt").type(STRING).optional().description("리마인드 시각"),
                                fieldWithPath("status").type(STRING).description("상태"),
                                fieldWithPath("createdAt").type(STRING).description("생성 시각")
                        )
                ));
    }

    @Test
    void 핀_장소수정_patch_200(RestDocumentationContextProvider restDocumentation) throws Exception {
        MockMvc mockMvc = mockMvc(restDocumentation);

        var me = docs.saveUser("pinEdit", "나");
        var peer = docs.saveUser("pinEdit2", "상대");
        String token = docs.issueTokenFor(me);

        Conversation c = conversationRepository.save(Conversation.direct());
        memberRepository.save(ConversationMember.join(c.getId(), me.getId()));
        memberRepository.save(ConversationMember.join(c.getId(), peer.getId()));

        ConversationPin pin = pinRepository.save(
                ConversationPin.createSchedule(
                        c.getId(),
                        me.getId(),
                        null,
                        "약속",
                        null,
                        LocalDateTime.now().plusDays(1).withHour(19).withMinute(0).withSecond(0).withNano(0)
                )
        );

        mockMvc.perform(patch("/api/pins/{pinId}", pin.getId())
                        .header(DocsTestSupport.headerName(), DocsTestSupport.auth(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "운동 약속",
                                  "placeText": "회사 앞",
                                  "remindMinutes": 30
                                }
                                """))
                .andExpect(status().isOk())
                .andDo(document("pins-update",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(
                                headerWithName(DocsTestSupport.headerName()).description("Bearer {accessToken}")
                        ),
                        pathParameters(
                                parameterWithName("pinId").description("핀 ID(UUID)")
                        ),
                        requestFields(
                                fieldWithPath("title").type(STRING).optional().description("수정할 제목"),
                                fieldWithPath("placeText").type(STRING).optional().description("장소 텍스트(빈 값이면 null 처리)"),
                                fieldWithPath("startAt").type(STRING).optional().description("수정할 시작 시각"),
                                fieldWithPath("remindMinutes").type(NUMBER).optional().description("리마인드 분(0,5,10,30,60)")
                        )
                ));
    }

    @Test
    void 핀_삭제_delete_204(RestDocumentationContextProvider restDocumentation) throws Exception {
        MockMvc mockMvc = mockMvc(restDocumentation);
        var me = docs.saveUser("pinDel", "나");
        var peer = docs.saveUser("pinDel2", "상대");
        String token = docs.issueTokenFor(me);
        Conversation c = conversationRepository.save(Conversation.direct());
        memberRepository.save(ConversationMember.join(c.getId(), me.getId()));
        memberRepository.save(ConversationMember.join(c.getId(), peer.getId()));
        ConversationPin pin = pinRepository.save(ConversationPin.createSchedule(c.getId(), me.getId(), null, "삭제할 액션", null, LocalDateTime.now().plusDays(1)));

        mockMvc.perform(delete("/api/pins/{pinId}", pin.getId())
                        .header(DocsTestSupport.headerName(), DocsTestSupport.auth(token)))
                .andExpect(status().isOk())
                .andDo(document("pins-delete",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(headerWithName(DocsTestSupport.headerName()).description("Bearer {accessToken}")),
                        pathParameters(parameterWithName("pinId").description("삭제할 핀 ID(UUID)"))
                ));
    }
}
