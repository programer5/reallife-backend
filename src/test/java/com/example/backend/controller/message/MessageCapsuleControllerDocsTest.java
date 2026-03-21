package com.example.backend.controller.message;

import com.example.backend.controller.DocsTestSupport;
import com.example.backend.controller.message.dto.MessageCapsuleUpdateRequest;
import com.example.backend.domain.message.Conversation;
import com.example.backend.domain.message.ConversationMember;
import com.example.backend.domain.message.Message;
import com.example.backend.domain.message.MessageCapsule;
import com.example.backend.repository.message.ConversationMemberRepository;
import com.example.backend.repository.message.ConversationRepository;
import com.example.backend.repository.message.MessageCapsuleRepository;
import com.example.backend.repository.message.MessageRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import static org.springframework.restdocs.payload.JsonFieldType.ARRAY;
import static org.springframework.restdocs.payload.JsonFieldType.BOOLEAN;
import static org.springframework.restdocs.payload.JsonFieldType.STRING;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@ExtendWith({RestDocumentationExtension.class, SpringExtension.class})
@Transactional
class MessageCapsuleControllerDocsTest {

    @Autowired WebApplicationContext context;
    @Autowired DocsTestSupport docs;
    @Autowired ObjectMapper objectMapper;
    @Autowired MessageCapsuleRepository capsuleRepository;
    @Autowired ConversationRepository conversationRepository;
    @Autowired ConversationMemberRepository memberRepository;
    @Autowired MessageRepository messageRepository;

    private MockMvc mockMvc(RestDocumentationContextProvider restDocumentation) {
        return MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .apply(documentationConfiguration(restDocumentation))
                .build();
    }

    @Test
    void 캡슐_생성_200(RestDocumentationContextProvider restDocumentation) throws Exception {
        MockMvc mockMvc = mockMvc(restDocumentation);
        var me = docs.saveUser("capsule-create", "나");
        var peer = docs.saveUser("capsule-create-peer", "상대");
        String token = docs.issueTokenFor(me);

        Conversation conversation = conversationRepository.saveAndFlush(Conversation.direct());
        memberRepository.saveAndFlush(ConversationMember.join(conversation.getId(), me.getId()));
        memberRepository.saveAndFlush(ConversationMember.join(conversation.getId(), peer.getId()));
        Message message = messageRepository.saveAndFlush(Message.text(conversation.getId(), me.getId(), "캡슐 기준 메시지"));

        mockMvc.perform(post("/api/capsules")
                        .queryParam("messageId", message.getId().toString())
                        .queryParam("conversationId", conversation.getId().toString())
                        .queryParam("title", "시험 끝나고 열기")
                        .queryParam("unlockAt", "2026-04-01T12:00:00")
                        .header(DocsTestSupport.headerName(), DocsTestSupport.auth(token)))
                .andExpect(status().isOk())
                .andDo(document("capsules-create",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(headerWithName(DocsTestSupport.headerName()).description("Bearer {accessToken}")),
                        queryParameters(
                                parameterWithName("messageId").description("기준 메시지 ID(UUID)"),
                                parameterWithName("conversationId").description("대화방 ID(UUID)"),
                                parameterWithName("title").description("캡슐 제목"),
                                parameterWithName("unlockAt").description("열릴 시각(ISO-8601)")
                        ),
                        responseFields(
                                fieldWithPath("capsuleId").type(STRING).description("생성된 캡슐 ID(UUID)")
                        )
                ));
    }

    @Test
    void 캡슐_목록_조회_200(RestDocumentationContextProvider restDocumentation) throws Exception {
        MockMvc mockMvc = mockMvc(restDocumentation);
        var me = docs.saveUser("capsule-list", "나");
        var peer = docs.saveUser("capsule-list-peer", "상대");
        String token = docs.issueTokenFor(me);
        Conversation conversation = conversationRepository.saveAndFlush(Conversation.direct());
        memberRepository.saveAndFlush(ConversationMember.join(conversation.getId(), me.getId()));
        memberRepository.saveAndFlush(ConversationMember.join(conversation.getId(), peer.getId()));
        var capsule = MessageCapsule.create(UUID.randomUUID(), conversation.getId(), me.getId(), "시험 끝나고 열기", LocalDateTime.now().plusDays(10));
        capsule.open();
        capsuleRepository.saveAndFlush(capsule);

        mockMvc.perform(get("/api/conversations/{conversationId}/capsules", conversation.getId())
                        .header(DocsTestSupport.headerName(), DocsTestSupport.auth(token)))
                .andExpect(status().isOk())
                .andDo(document("capsules-list",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(headerWithName(DocsTestSupport.headerName()).description("Bearer {accessToken}")),
                        pathParameters(parameterWithName("conversationId").description("대화방 ID(UUID)")),
                        responseFields(
                                fieldWithPath("conversationId").type(STRING).description("대화방 ID(UUID)"),
                                fieldWithPath("items").type(ARRAY).description("캡슐 목록"),
                                fieldWithPath("items[].capsuleId").type(STRING).description("캡슐 ID(UUID)"),
                                fieldWithPath("items[].messageId").type(STRING).description("기준 메시지 ID(UUID)"),
                                fieldWithPath("items[].creatorId").type(STRING).description("생성자 ID(UUID)"),
                                fieldWithPath("items[].title").type(STRING).description("캡슐 제목"),
                                fieldWithPath("items[].unlockAt").type(STRING).description("열릴 시각"),
                                fieldWithPath("items[].openedAt").type(STRING).optional().description("열린 시각"),
                                fieldWithPath("items[].opened").type(BOOLEAN).description("열림 여부")
                        )
                ));
    }

    @Test
    void 캡슐_열기_200(RestDocumentationContextProvider restDocumentation) throws Exception {
        MockMvc mockMvc = mockMvc(restDocumentation);
        var me = docs.saveUser("capsule-open", "나");
        var peer = docs.saveUser("capsule-open-peer", "상대");
        String token = docs.issueTokenFor(me);
        Conversation conversation = conversationRepository.saveAndFlush(Conversation.direct());
        memberRepository.saveAndFlush(ConversationMember.join(conversation.getId(), me.getId()));
        memberRepository.saveAndFlush(ConversationMember.join(conversation.getId(), peer.getId()));
        var capsule = capsuleRepository.saveAndFlush(MessageCapsule.create(UUID.randomUUID(), conversation.getId(), me.getId(), "열어봐", LocalDateTime.now().minusDays(1)));

        mockMvc.perform(post("/api/capsules/{capsuleId}/open", capsule.getId())
                        .header(DocsTestSupport.headerName(), DocsTestSupport.auth(token)))
                .andExpect(status().isOk())
                .andDo(document("capsules-open",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(headerWithName(DocsTestSupport.headerName()).description("Bearer {accessToken}")),
                        pathParameters(parameterWithName("capsuleId").description("캡슐 ID(UUID)"))
                ));
    }

    @Test
    void 캡슐_수정_200(RestDocumentationContextProvider restDocumentation) throws Exception {
        MockMvc mockMvc = mockMvc(restDocumentation);
        var me = docs.saveUser("capsule-update", "나");
        String token = docs.issueTokenFor(me);
        var capsule = capsuleRepository.saveAndFlush(MessageCapsule.create(UUID.randomUUID(), UUID.randomUUID(), me.getId(), "열어봐", LocalDateTime.now().plusDays(1)));

        mockMvc.perform(patch("/api/capsules/{capsuleId}", capsule.getId())
                        .header(DocsTestSupport.headerName(), DocsTestSupport.auth(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new MessageCapsuleUpdateRequest("수정된 캡슐", "2026-05-01T12:00:00"))))
                .andExpect(status().isOk())
                .andDo(document("capsules-update",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(headerWithName(DocsTestSupport.headerName()).description("Bearer {accessToken}")),
                        pathParameters(parameterWithName("capsuleId").description("캡슐 ID(UUID)")),
                        requestFields(
                                fieldWithPath("title").type(STRING).optional().description("수정할 캡슐 제목"),
                                fieldWithPath("unlockAt").type(STRING).optional().description("수정할 열릴 시각(ISO-8601)")
                        )
                ));
    }

    @Test
    void 캡슐_삭제_204(RestDocumentationContextProvider restDocumentation) throws Exception {
        MockMvc mockMvc = mockMvc(restDocumentation);
        var me = docs.saveUser("capsule-delete", "나");
        String token = docs.issueTokenFor(me);
        var capsule = capsuleRepository.saveAndFlush(MessageCapsule.create(UUID.randomUUID(), UUID.randomUUID(), me.getId(), "삭제할 캡슐", LocalDateTime.now().plusDays(1)));

        mockMvc.perform(delete("/api/capsules/{capsuleId}", capsule.getId())
                        .header(DocsTestSupport.headerName(), DocsTestSupport.auth(token)))
                .andExpect(status().isOk())
                .andDo(document("capsules-delete",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(headerWithName(DocsTestSupport.headerName()).description("Bearer {accessToken}")),
                        pathParameters(parameterWithName("capsuleId").description("삭제할 캡슐 ID(UUID)"))
                ));
    }
}
