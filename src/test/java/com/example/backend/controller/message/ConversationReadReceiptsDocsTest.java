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
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@ExtendWith({RestDocumentationExtension.class, SpringExtension.class})
@Transactional
class ConversationReadReceiptsDocsTest {

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
    void 대화방_읽음표시_조회_readReceipts_200(RestDocumentationContextProvider restDocumentation) throws Exception {
        MockMvc mockMvc = mockMvc(restDocumentation);

        var me = docs.saveUser("rr1", "나");
        var peer = docs.saveUser("rr2", "상대");
        String meToken = docs.issueTokenFor(me);
        String peerToken = docs.issueTokenFor(peer);

        Conversation c = conversationRepository.save(Conversation.direct());
        memberRepository.save(ConversationMember.join(c.getId(), me.getId()));
        memberRepository.save(ConversationMember.join(c.getId(), peer.getId()));

        // 내가 메시지 1개 보냄 (상대가 읽으면 peer.lastReadAt가 이 createdAt으로 잡힘)
        var m1 = messageRepository.save(Message.text(c.getId(), me.getId(), "hello"));
        c.updateLastMessage(m1.getId(), m1.getCreatedAt(), "hello");

        // 상대가 읽음 처리 → peer.lastReadAt 세팅
        mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .post("/api/conversations/{conversationId}/read", c.getId())
                                .header(DocsTestSupport.headerName(), DocsTestSupport.auth(peerToken))
                )
                .andDo(print())
                .andExpect(status().isOk());

        // 내가 read-receipts 조회
        mockMvc.perform(get("/api/conversations/{conversationId}/read-receipts", c.getId())
                        .header(DocsTestSupport.headerName(), DocsTestSupport.auth(meToken)))
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(document("conversations-read-receipts-200",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(headerWithName(DocsTestSupport.headerName()).description("Bearer {accessToken}")),
                        pathParameters(parameterWithName("conversationId").description("대화방 ID(UUID)")),
                        responseFields(
                                fieldWithPath("items").type(ARRAY).description("대화방 멤버들의 읽음 상태 목록"),
                                fieldWithPath("items[].userId").type(STRING).description("유저 ID(UUID)"),
                                fieldWithPath("items[].lastReadAt").optional().type(STRING).description("마지막 읽음 시각(없으면 null)")
                        )
                ));
    }
}