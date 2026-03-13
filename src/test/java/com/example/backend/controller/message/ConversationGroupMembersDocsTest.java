
package com.example.backend.controller.message;

import com.example.backend.controller.DocsTestSupport;
import com.example.backend.domain.message.ConversationMember;
import com.example.backend.repository.message.ConversationMemberRepository;
import com.example.backend.repository.message.ConversationRepository;
import com.example.backend.service.message.ConversationService;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;
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

import java.util.List;

import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.payload.JsonFieldType.ARRAY;
import static org.springframework.restdocs.payload.JsonFieldType.STRING;
import static org.springframework.restdocs.payload.JsonFieldType.OBJECT;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@ExtendWith({RestDocumentationExtension.class, SpringExtension.class})
@Transactional
class ConversationGroupMembersDocsTest {

    @Autowired WebApplicationContext context;
    @Autowired DocsTestSupport docs;
    @Autowired ConversationService conversationService;

    private MockMvc mockMvc(RestDocumentationContextProvider restDocumentation) {
        return MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .apply(documentationConfiguration(restDocumentation))
                .build();
    }

    @Test
    void 그룹_대화방_멤버_조회_성공_200(RestDocumentationContextProvider restDocumentation) throws Exception {
        var me = docs.saveUser("gmem", "그룹방장");
        var a = docs.saveUser("gmem1", "멤버1");
        var b = docs.saveUser("gmem2", "멤버2");
        String token = docs.issueTokenFor(me);

        var conversationId = conversationService.createGroup(me.getId(), "주말 스터디", List.of(a.getId(), b.getId()), null);

        mockMvc(restDocumentation)
                .perform(get("/api/conversations/{conversationId}/members", conversationId)
                        .header(HttpHeaders.AUTHORIZATION, DocsTestSupport.auth(token)))
                .andExpect(status().isOk())
                .andDo(document("conversation-members-get",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(
                                headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer {accessToken}")
                        ),
                        pathParameters(
                                parameterWithName("conversationId").description("그룹 대화방 ID(UUID)")
                        ),
                        responseFields(
                                fieldWithPath("conversationId").type(STRING).description("대화방 ID(UUID)"),
                                fieldWithPath("items").type(ARRAY).description("그룹 멤버 목록"),
                                fieldWithPath("items[].userId").type(STRING).description("사용자 ID(UUID)"),
                                fieldWithPath("items[].handle").optional().type(STRING).description("사용자 handle"),
                                fieldWithPath("items[].nickname").optional().type(STRING).description("표시 이름"),
                                fieldWithPath("items[].profileImageUrl").optional().type(STRING).description("프로필 이미지 URL")
                        )
                ));
    }
}
