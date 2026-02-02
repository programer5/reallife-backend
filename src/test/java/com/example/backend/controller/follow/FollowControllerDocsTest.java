package com.example.backend.controller.follow;

import com.example.backend.controller.DocsTestSupport;
import com.example.backend.restdocs.ErrorResponseSnippet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ExtendWith({RestDocumentationExtension.class, SpringExtension.class})
@Transactional
class FollowControllerDocsTest {

    @Autowired WebApplicationContext context;
    @Autowired DocsTestSupport docs;

    private MockMvc mockMvc(RestDocumentationContextProvider restDocumentation) {
        return MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .apply(documentationConfiguration(restDocumentation))
                .build();
    }

    @Test
    void 팔로우_성공_204(RestDocumentationContextProvider restDocumentation) throws Exception {
        var me = docs.saveUser("followme", "팔로워");
        var target = docs.saveUser("followtarget", "타겟");
        String token = docs.issueTokenFor(me);

        mockMvc(restDocumentation)
                .perform(post("/api/follows/{targetUserId}", target.getId())
                        .header(HttpHeaders.AUTHORIZATION, DocsTestSupport.auth(token)))
                .andExpect(status().isNoContent())
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andDo(document("follows-create-204",
                        requestHeaders(
                                headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer {accessToken}")
                        ),
                        pathParameters(
                                parameterWithName("targetUserId").description("팔로우 대상 사용자 ID(UUID)")
                        )
                ));
    }

    @Test
    void 팔로우_실패_자기자신_팔로우_400(RestDocumentationContextProvider restDocumentation) throws Exception {
        var me = docs.saveUser("self", "나");
        String token = docs.issueTokenFor(me);

        mockMvc(restDocumentation)
                .perform(post("/api/follows/{targetUserId}", me.getId())
                        .header(HttpHeaders.AUTHORIZATION, DocsTestSupport.auth(token)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("FOLLOW_CANNOT_FOLLOW_SELF"))
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andDo(document("follows-400-self",
                        requestHeaders(
                                headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer {accessToken}")
                        ),
                        pathParameters(
                                parameterWithName("targetUserId").description("팔로우 대상 사용자 ID(UUID)")
                        ),
                        org.springframework.restdocs.payload.PayloadDocumentation.responseFields(ErrorResponseSnippet.common())
                ));
    }
}
