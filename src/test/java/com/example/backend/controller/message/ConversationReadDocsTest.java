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
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.JsonFieldType.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@ExtendWith({RestDocumentationExtension.class, SpringExtension.class})
@Transactional
class ConversationReadDocsTest {

    @Autowired WebApplicationContext context;
    @Autowired DocsTestSupport docs;

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
    void 대화방_읽음처리_후_unreadCount_감소_예시(RestDocumentationContextProvider restDocumentation) throws Exception {
        MockMvc mockMvc = mockMvc(restDocumentation);

        var me = docs.saveUser("dmRead", "나");
        var peer = docs.saveUser("dmRead2", "상대");
        String token = docs.issueTokenFor(me);

        Conversation c = conversationRepository.save(Conversation.direct());
        memberRepository.save(ConversationMember.join(c.getId(), me.getId()));
        memberRepository.save(ConversationMember.join(c.getId(), peer.getId()));

        // 상대가 메시지 2개 보냄 -> unreadCount=2 기대
        var m1 = messageRepository.save(Message.text(c.getId(), peer.getId(), "msg1"));
        var m2 = messageRepository.save(Message.text(c.getId(), peer.getId(), "msg2"));
        c.updateLastMessage(m2.getId(), m2.getCreatedAt(), "msg2");

        // 1) 읽기 전 목록 조회 (unreadCount > 0)
        mockMvc.perform(get("/api/conversations")
                        .param("size", "20")
                        .header(DocsTestSupport.headerName(), DocsTestSupport.auth(token)))
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(document("conversations-unread-before",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(headerWithName(DocsTestSupport.headerName()).description("Bearer {accessToken}")),
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
                                fieldWithPath("items[].peerUser.profileImageUrl").optional().type(NULL).description("프로필 이미지(현재 null)"),
                                fieldWithPath("items[].lastMessagePreview").optional().type(STRING).description("마지막 메시지 미리보기"),
                                fieldWithPath("items[].lastMessageAt").optional().type(STRING).description("마지막 메시지 시각"),
                                fieldWithPath("items[].unreadCount").type(NUMBER).description("안읽은 메시지 수"),
                                fieldWithPath("nextCursor").optional().type(STRING).description("다음 페이지 커서"),
                                fieldWithPath("hasNext").type(BOOLEAN).description("다음 페이지 존재 여부")
                        )
                ));

        // 2) 읽음 처리 호출
        mockMvc.perform(post("/api/conversations/{conversationId}/read", c.getId())
                        .header(DocsTestSupport.headerName(), DocsTestSupport.auth(token)))
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(document("conversations-read",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(headerWithName(DocsTestSupport.headerName()).description("Bearer {accessToken}")),
                        pathParameters(parameterWithName("conversationId").description("대화방 ID(UUID)"))
                ));

        // 3) 읽기 후 목록 조회 (unreadCount=0 기대)
        mockMvc.perform(get("/api/conversations")
                        .param("size", "20")
                        .header(DocsTestSupport.headerName(), DocsTestSupport.auth(token)))
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(document("conversations-unread-after",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(headerWithName(DocsTestSupport.headerName()).description("Bearer {accessToken}")),
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
                                fieldWithPath("items[].peerUser.profileImageUrl").optional().type(NULL).description("프로필 이미지(현재 null)"),
                                fieldWithPath("items[].lastMessagePreview").optional().type(STRING).description("마지막 메시지 미리보기"),
                                fieldWithPath("items[].lastMessageAt").optional().type(STRING).description("마지막 메시지 시각"),
                                fieldWithPath("items[].unreadCount").type(NUMBER).description("안읽은 메시지 수"),
                                fieldWithPath("nextCursor").optional().type(STRING).description("다음 페이지 커서"),
                                fieldWithPath("hasNext").type(BOOLEAN).description("다음 페이지 존재 여부")
                        )
                ));
    }
}