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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ExtendWith({RestDocumentationExtension.class, SpringExtension.class})
@Transactional
class FollowControllerDocsTest {

    @Autowired private WebApplicationContext context;
    @Autowired private DocsTestSupport docs;

    private MockMvc mockMvc(RestDocumentationContextProvider restDocumentation) {
        return MockMvcBuilders.webAppContextSetup(context)
                .apply(documentationConfiguration(restDocumentation))
                .build();
    }

    @Test
    void 팔로우_성공_204(RestDocumentationContextProvider restDocumentation) throws Exception {
        MockMvc mockMvc = mockMvc(restDocumentation);

        var me = docs.saveUser("followme", "팔로워");
        var target = docs.saveUser("followtarget", "타겟");

        String token = docs.issueTokenFor(me);

        mockMvc.perform(post("/api/follows/{targetUserId}", target.getId())
                        .header(HttpHeaders.AUTHORIZATION, DocsTestSupport.auth(token)))
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andExpect(status().isNoContent())
                .andDo(document("follows-create",
                        requestHeaders(
                                headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer {accessToken}")
                        ),
                        pathParameters(
                                parameterWithName("targetUserId").description("팔로우 대상 사용자 ID(UUID)")
                        )
                ));
    }

    @Test
    void 언팔로우_성공_204(RestDocumentationContextProvider restDocumentation) throws Exception {
        MockMvc mockMvc = mockMvc(restDocumentation);

        var me = docs.saveUser("unfollowme", "언팔로워");
        var target = docs.saveUser("unfollowtarget", "타겟");

        String token = docs.issueTokenFor(me);

        // 먼저 팔로우 해두고
        mockMvc.perform(post("/api/follows/{targetUserId}", target.getId())
                        .header(HttpHeaders.AUTHORIZATION, DocsTestSupport.auth(token)))
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andExpect(status().isNoContent());

        // 언팔로우
        mockMvc.perform(delete("/api/follows/{targetUserId}", target.getId())
                        .header(HttpHeaders.AUTHORIZATION, DocsTestSupport.auth(token)))
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andExpect(status().isNoContent())
                .andDo(document("follows-delete",
                        requestHeaders(
                                headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer {accessToken}")
                        ),
                        pathParameters(
                                parameterWithName("targetUserId").description("언팔로우 대상 사용자 ID(UUID)")
                        )
                ));
    }

    @Test
    void 팔로우_실패_자기자신_400(RestDocumentationContextProvider restDocumentation) throws Exception {
        MockMvc mockMvc = mockMvc(restDocumentation);

        var me = docs.saveUser("self", "나");
        String token = docs.issueTokenFor(me);

        mockMvc.perform(post("/api/follows/{targetUserId}", me.getId())
                        .header(HttpHeaders.AUTHORIZATION, DocsTestSupport.auth(token)))
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("FOLLOW_CANNOT_FOLLOW_SELF")) // ✅ 네 실제 코드에 맞춰
                .andDo(document("follows-create-400-self",
                        requestHeaders(
                                headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer {accessToken}")
                        ),
                        pathParameters(
                                parameterWithName("targetUserId").description("팔로우 대상 사용자 ID(UUID)")
                        ),
                        org.springframework.restdocs.payload.PayloadDocumentation.responseFields(
                                ErrorResponseSnippet.common()
                        )
                ));
    }
}