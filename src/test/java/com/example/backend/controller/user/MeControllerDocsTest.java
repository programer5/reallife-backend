package com.example.backend.controller.user;

import com.example.backend.controller.DocsTestSupport;
import com.example.backend.restdocs.ErrorResponseSnippet;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.payload.JsonFieldType.NUMBER;
import static org.springframework.restdocs.payload.JsonFieldType.STRING;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@ExtendWith({RestDocumentationExtension.class, SpringExtension.class})
@Transactional
class MeControllerDocsTest {

    @Autowired WebApplicationContext context;
    @Autowired ObjectMapper objectMapper;
    @Autowired DocsTestSupport docs;

    private MockMvc mockMvc(RestDocumentationContextProvider restDocumentation) {
        return MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .apply(documentationConfiguration(restDocumentation))
                .build();
    }

    @Test
    void 내정보_조회_실패_토큰없음_401(RestDocumentationContextProvider restDocumentation) throws Exception {
        mockMvc(restDocumentation)
                .perform(get("/api/me"))
                .andExpect(status().isUnauthorized())
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andDo(document("me-401-unauthorized",
                        responseFields(ErrorResponseSnippet.common())
                ));
    }

    @Test
    void 내정보_조회_성공_토큰있음_200(RestDocumentationContextProvider restDocumentation) throws Exception {
        var me = docs.saveUser("me", "시드유저");
        String token = docs.issueTokenFor(me);

        mockMvc(restDocumentation)
                .perform(get("/api/me")
                        .header(HttpHeaders.AUTHORIZATION, DocsTestSupport.auth(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())       // ✅ id 존재
                .andExpect(jsonPath("$.email").exists())
                .andExpect(jsonPath("$.handle").exists())
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andDo(document("me-200-ok",
                        requestHeaders(
                                headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer {accessToken}")
                        ),
                        responseFields(
                                fieldWithPath("id").type(STRING).description("내 사용자 ID(UUID)"),
                                fieldWithPath("email").type(STRING).description("내 이메일"),
                                fieldWithPath("handle").type(STRING).description("내 아이디(핸들)"),
                                fieldWithPath("name").type(STRING).description("내 이름"),
                                fieldWithPath("followerCount").type(NUMBER).description("팔로워 수"),
                                fieldWithPath("tier").type(STRING).description("팔로워 티어")
                        )
                ));
    }
}