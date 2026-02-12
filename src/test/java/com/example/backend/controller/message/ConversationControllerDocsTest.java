package com.example.backend.controller.message;

import com.example.backend.controller.DocsTestSupport;
import com.example.backend.domain.message.Conversation;
import com.example.backend.domain.message.ConversationMember;
import com.example.backend.domain.message.Message;
import com.example.backend.repository.message.ConversationMemberRepository;
import com.example.backend.repository.message.ConversationRepository;
import com.example.backend.repository.message.MessageRepository;
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

import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.JsonFieldType.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*; // ✅ queryParameters, parameterWithName 등
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@ExtendWith({RestDocumentationExtension.class, SpringExtension.class})
@Transactional
class ConversationControllerDocsTest {

    @Autowired private WebApplicationContext context;
    @Autowired private DocsTestSupport docs;

    @Autowired private ConversationRepository conversationRepository;
    @Autowired private ConversationMemberRepository memberRepository;
    @Autowired private MessageRepository messageRepository;

    private MockMvc mockMvc(RestDocumentationContextProvider restDocumentation) {
        return MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .apply(documentationConfiguration(restDocumentation))
                .build();
    }

    @Test
    void 대화방_목록_조회_성공_200(RestDocumentationContextProvider restDocumentation) throws Exception {
        MockMvc mockMvc = mockMvc(restDocumentation);

        var me = docs.saveUser("dm", "나");
        var peer = docs.saveUser("dm2", "상대");
        String token = docs.issueTokenFor(me);

        Conversation c = conversationRepository.save(Conversation.direct());
        memberRepository.save(ConversationMember.join(c.getId(), me.getId()));
        memberRepository.save(ConversationMember.join(c.getId(), peer.getId()));

        var msg = messageRepository.save(Message.text(c.getId(), peer.getId(), "hello dm"));
        c.updateLastMessage(msg.getId(), msg.getCreatedAt(), "hello dm"); // ✅ lastMessage denorm 채우기

        mockMvc.perform(get("/api/conversations")
                        .param("size", "20")
                        .header(DocsTestSupport.headerName(), DocsTestSupport.auth(token))
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(document("conversations-list",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(
                                headerWithName(DocsTestSupport.headerName()).description("Bearer {accessToken}")
                        ),
                        queryParameters( // ✅ requestParameters 말고 queryParameters
                                parameterWithName("cursor").optional()
                                        .description("커서(없으면 첫 페이지). 형식: {epochMillis}_{conversationId}"),
                                parameterWithName("size").optional()
                                        .description("페이지 크기(default 20, max 50)")
                        ),
                        responseFields(
                                fieldWithPath("items").type(ARRAY).description("대화방 목록"),
                                fieldWithPath("items[].conversationId").type(STRING).description("대화방 ID(UUID)"),
                                fieldWithPath("items[].peerUser").type(OBJECT).description("상대 유저"),
                                fieldWithPath("items[].peerUser.userId").type(STRING).description("상대 유저 ID(UUID)"),
                                fieldWithPath("items[].peerUser.nickname").type(STRING).description("상대 유저 표시명(User.name)"),
                                fieldWithPath("items[].peerUser.profileImageUrl").optional().type(NULL)
                                        .description("상대 유저 프로필 이미지 URL(현재 null)"),
                                fieldWithPath("items[].lastMessagePreview").optional().type(STRING)
                                        .description("마지막 메시지 미리보기"),
                                fieldWithPath("items[].lastMessageAt").optional().type(STRING)
                                        .description("마지막 메시지 시각(ISO-8601)"),
                                fieldWithPath("items[].unreadCount").type(NUMBER).description("안읽은 메시지 수"),
                                fieldWithPath("nextCursor").optional().type(STRING).description("다음 페이지 커서"),
                                fieldWithPath("hasNext").type(BOOLEAN).description("다음 페이지 존재 여부")
                        )
                ));
    }

    @Test
    void 대화방목록_조회_200_next_page_example(RestDocumentationContextProvider restDocumentation) throws Exception {
        MockMvc mockMvc = mockMvc(restDocumentation);

        var me = docs.saveUser("dmNextA", "나");
        var peer1 = docs.saveUser("dmNextB", "상대1");
        var peer2 = docs.saveUser("dmNextC", "상대2");
        var peer3 = docs.saveUser("dmNextD", "상대3");
        String token = docs.issueTokenFor(me);

        Conversation c1 = conversationRepository.saveAndFlush(Conversation.direct());
        memberRepository.saveAndFlush(ConversationMember.join(c1.getId(), me.getId()));
        memberRepository.saveAndFlush(ConversationMember.join(c1.getId(), peer1.getId()));
        var msg1 = messageRepository.saveAndFlush(Message.text(c1.getId(), peer1.getId(), "hello 1"));
        c1.updateLastMessage(msg1.getId(), msg1.getCreatedAt(), "hello 1");
        conversationRepository.saveAndFlush(c1);
        Thread.sleep(2);

        Conversation c2 = conversationRepository.saveAndFlush(Conversation.direct());
        memberRepository.saveAndFlush(ConversationMember.join(c2.getId(), me.getId()));
        memberRepository.saveAndFlush(ConversationMember.join(c2.getId(), peer2.getId()));
        var msg2 = messageRepository.saveAndFlush(Message.text(c2.getId(), peer2.getId(), "hello 2"));
        c2.updateLastMessage(msg2.getId(), msg2.getCreatedAt(), "hello 2");
        conversationRepository.saveAndFlush(c2);
        Thread.sleep(2);

        Conversation c3 = conversationRepository.saveAndFlush(Conversation.direct());
        memberRepository.saveAndFlush(ConversationMember.join(c3.getId(), me.getId()));
        memberRepository.saveAndFlush(ConversationMember.join(c3.getId(), peer3.getId()));
        var msg3 = messageRepository.saveAndFlush(Message.text(c3.getId(), peer3.getId(), "hello 3"));
        c3.updateLastMessage(msg3.getId(), msg3.getCreatedAt(), "hello 3");
        conversationRepository.saveAndFlush(c3);

        var first = mockMvc.perform(get("/api/conversations")
                        .param("size", "1")
                        .header(DocsTestSupport.headerName(), DocsTestSupport.auth(token)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.nextCursor").isNotEmpty())
                .andDo(document("conversations-list-200-next-page",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(
                                headerWithName(DocsTestSupport.headerName()).description("Bearer {accessToken}")
                        ),
                        queryParameters(
                                parameterWithName("cursor").optional().description("커서(없으면 첫 페이지)"),
                                parameterWithName("size").optional().description("페이지 크기(default 20, max 50)")
                        ),
                        responseFields(
                                fieldWithPath("items").type(ARRAY).description("대화방 목록"),
                                fieldWithPath("items[].conversationId").type(STRING).description("대화방 ID(UUID)"),
                                fieldWithPath("items[].peerUser").type(OBJECT).description("상대 유저"),
                                fieldWithPath("items[].peerUser.userId").type(STRING).description("상대 유저 ID(UUID)"),
                                fieldWithPath("items[].peerUser.nickname").type(STRING).description("상대 유저 표시명(User.name)"),
                                fieldWithPath("items[].peerUser.profileImageUrl").optional().type(NULL).description("상대 유저 프로필 이미지 URL(현재 null)"),
                                fieldWithPath("items[].lastMessagePreview").optional().type(STRING).description("마지막 메시지 미리보기"),
                                fieldWithPath("items[].lastMessageAt").optional().type(STRING).description("마지막 메시지 시각(ISO-8601)"),
                                fieldWithPath("items[].unreadCount").type(NUMBER).description("안읽은 메시지 수"),
                                fieldWithPath("nextCursor").optional().type(STRING).description("다음 페이지 커서"),
                                fieldWithPath("hasNext").type(BOOLEAN).description("다음 페이지 존재 여부")
                        )
                ))
                .andReturn();

        String body = first.getResponse().getContentAsString();
        String nextCursor = com.jayway.jsonpath.JsonPath.read(body, "$.nextCursor");

        mockMvc.perform(get("/api/conversations")
                        .param("cursor", nextCursor)
                        .param("size", "1")
                        .header(DocsTestSupport.headerName(), DocsTestSupport.auth(token)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.items[0].conversationId").exists()) // ✅ 안정화
                .andDo(document("conversations-list-200-next-page-followup",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(
                                headerWithName(DocsTestSupport.headerName()).description("Bearer {accessToken}")
                        ),
                        queryParameters(
                                parameterWithName("cursor").description("이전 응답의 nextCursor 값(opaque)"),
                                parameterWithName("size").optional().description("페이지 크기(default 20, max 50)")
                        ),
                        responseFields(
                                fieldWithPath("items").type(ARRAY).description("대화방 목록"),
                                fieldWithPath("items[].conversationId").type(STRING).description("대화방 ID(UUID)"),
                                fieldWithPath("items[].peerUser").type(OBJECT).description("상대 유저"),
                                fieldWithPath("items[].peerUser.userId").type(STRING).description("상대 유저 ID(UUID)"),
                                fieldWithPath("items[].peerUser.nickname").type(STRING).description("상대 유저 표시명(User.name)"),
                                fieldWithPath("items[].peerUser.profileImageUrl").optional().type(NULL).description("상대 유저 프로필 이미지 URL(현재 null)"),
                                fieldWithPath("items[].lastMessagePreview").optional().type(STRING).description("마지막 메시지 미리보기"),
                                fieldWithPath("items[].lastMessageAt").optional().type(STRING).description("마지막 메시지 시각(ISO-8601)"),
                                fieldWithPath("items[].unreadCount").type(NUMBER).description("안읽은 메시지 수"),
                                fieldWithPath("nextCursor").optional().type(STRING).description("다음 페이지 커서"),
                                fieldWithPath("hasNext").type(BOOLEAN).description("다음 페이지 존재 여부")
                        )
                ));
    }
}