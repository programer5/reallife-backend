package com.example.backend.controller.message;

import com.example.backend.controller.DocsTestSupport;
import com.example.backend.domain.message.Conversation;
import com.example.backend.domain.message.ConversationMember;
import com.example.backend.repository.message.ConversationMemberRepository;
import com.example.backend.repository.message.ConversationRepository;
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

import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.JsonFieldType.STRING;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@ExtendWith({RestDocumentationExtension.class, SpringExtension.class})
@Transactional
class ConversationReadStateDocsTest {

    @Autowired WebApplicationContext context;
    @Autowired DocsTestSupport docs;

    @Autowired ConversationRepository conversationRepository;
    @Autowired ConversationMemberRepository memberRepository;

    private MockMvc mockMvc(RestDocumentationContextProvider restDocumentation) {
        return MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .apply(documentationConfiguration(restDocumentation))
                .build();
    }

    @Test
    void 대화방_읽음상태_조회_readState_200(RestDocumentationContextProvider restDocumentation) throws Exception {
        MockMvc mockMvc = mockMvc(restDocumentation);

        var me = docs.saveUser("readState", "나");
        var peer = docs.saveUser("readState2", "상대");
        String token = docs.issueTokenFor(me);

        Conversation c = conversationRepository.save(Conversation.direct());

        ConversationMember meMember = memberRepository.save(ConversationMember.join(c.getId(), me.getId()));
        memberRepository.save(ConversationMember.join(c.getId(), peer.getId()));

        // lastReadAt 값이 있는 케이스를 문서화 (unread divider 계산용)
        LocalDateTime lastReadAt = LocalDateTime.now().minusMinutes(3);
        meMember.markReadAt(lastReadAt);
        memberRepository.save(meMember);

        mockMvc.perform(get("/api/conversations/{conversationId}/read-state", c.getId())
                        .header(DocsTestSupport.headerName(), DocsTestSupport.auth(token)))
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(document("conversations-read-state-200",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(headerWithName(DocsTestSupport.headerName()).description("Bearer {accessToken}")),
                        pathParameters(parameterWithName("conversationId").description("대화방 ID(UUID)")),
                        responseFields(
                                fieldWithPath("lastReadAt").optional().type(STRING).description("내 마지막 읽음 시각(없으면 null)")
                        )
                ));
    }
}