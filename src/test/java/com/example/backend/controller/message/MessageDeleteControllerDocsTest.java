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
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@ExtendWith({RestDocumentationExtension.class, SpringExtension.class})
@Transactional
class MessageDeleteControllerDocsTest {

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
    void 메시지_나만삭제_hide_성공_200(RestDocumentationContextProvider restDocumentation) throws Exception {
        MockMvc mockMvc = mockMvc(restDocumentation);

        var me = docs.saveUser("hide", "나");
        var peer = docs.saveUser("hide2", "상대");
        String token = docs.issueTokenFor(me);

        Conversation c = conversationRepository.save(Conversation.direct());
        memberRepository.save(ConversationMember.join(c.getId(), me.getId()));
        memberRepository.save(ConversationMember.join(c.getId(), peer.getId()));

        Message msg = messageRepository.save(Message.text(c.getId(), peer.getId(), "hello"));

        mockMvc.perform(post("/api/messages/{messageId}/hide", msg.getId())
                        .header(DocsTestSupport.headerName(), DocsTestSupport.auth(token))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(document("message-hide",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(
                                headerWithName(DocsTestSupport.headerName()).description("Bearer {accessToken}")
                        ),
                        pathParameters(
                                parameterWithName("messageId").description("메시지 ID(UUID)")
                        )
                ));
    }

    @Test
    void 메시지_모두삭제_delete_for_all_성공_200(RestDocumentationContextProvider restDocumentation) throws Exception {
        MockMvc mockMvc = mockMvc(restDocumentation);

        var me = docs.saveUser("del", "나");
        var peer = docs.saveUser("del2", "상대");
        String token = docs.issueTokenFor(me);

        Conversation c = conversationRepository.save(Conversation.direct());
        memberRepository.save(ConversationMember.join(c.getId(), me.getId()));
        memberRepository.save(ConversationMember.join(c.getId(), peer.getId()));

        Message msg = messageRepository.save(Message.text(c.getId(), me.getId(), "hello")); // ✅ 내가 보낸 메시지

        mockMvc.perform(post("/api/messages/{messageId}/delete-for-all", msg.getId())
                        .header(DocsTestSupport.headerName(), DocsTestSupport.auth(token))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(document("message-delete-for-all",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(
                                headerWithName(DocsTestSupport.headerName()).description("Bearer {accessToken}")
                        ),
                        pathParameters(
                                parameterWithName("messageId").description("메시지 ID(UUID)")
                        )
                ));
    }
}