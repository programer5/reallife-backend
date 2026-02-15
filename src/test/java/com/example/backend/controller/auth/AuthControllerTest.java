package com.example.backend.controller.auth;

import com.example.backend.controller.DocsTestSupport;
import com.example.backend.restdocs.ErrorResponseSnippet;
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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.payload.JsonFieldType.STRING;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Auth API REST Docs
 *
 * - /api/auth/login (Bearer)
 * - /api/auth/refresh (rotation)
 * - /api/auth/refresh (reuse -> 401)
 * - /api/auth/logout-all (전체 기기 로그아웃)
 */
@SpringBootTest
@ActiveProfiles("test")
@ExtendWith({RestDocumentationExtension.class, SpringExtension.class})
@Transactional
class AuthControllerTest {

    @Autowired private WebApplicationContext context;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private DocsTestSupport docs;

    private MockMvc mockMvc(RestDocumentationContextProvider restDocumentation) {
        return MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .apply(documentationConfiguration(restDocumentation))
                .build();
    }

    // ---------- helpers ----------

    private record SignupInfo(String email, String handle) {}

    private SignupInfo signup(MockMvc mockMvc) throws Exception {
        String email = "auth+" + UUID.randomUUID() + "@test.com";
        String handle = "user_" + UUID.randomUUID().toString().substring(0, 8);

        var req = new HashMap<String, Object>();
        req.put("email", email);
        req.put("handle", handle);
        req.put("password", "password1234");
        req.put("name", "테스트유저");

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        return new SignupInfo(email, handle);
    }

    private Map<String, Object> login(MockMvc mockMvc, String email) throws Exception {
        var req = new HashMap<String, Object>();
        req.put("email", email);
        req.put("password", "password1234");

        var res = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readValue(res.getResponse().getContentAsByteArray(), Map.class);
    }

    // ---------- tests ----------

    @Test
    void 로그인_API_성공_Bearer_토큰반환(RestDocumentationContextProvider restDocumentation) throws Exception {
        MockMvc mockMvc = mockMvc(restDocumentation);

        var user = signup(mockMvc);

        var req = new HashMap<String, Object>();
        req.put("email", user.email());
        req.put("password", "password1234");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andDo(document("auth-login",
                        requestFields(
                                fieldWithPath("email").type(STRING).description("이메일"),
                                fieldWithPath("password").type(STRING).description("비밀번호")
                        ),
                        responseFields(
                                fieldWithPath("accessToken").type(STRING).description("Access Token (JWT)"),
                                fieldWithPath("refreshToken").type(STRING).description("Refresh Token (opaque)"),
                                fieldWithPath("tokenType").type(STRING).description("토큰 타입 (Bearer)")
                        )
                ));
    }

    @Test
    void 토큰재발급_API_성공_Refresh_Rotation(RestDocumentationContextProvider restDocumentation) throws Exception {
        MockMvc mockMvc = mockMvc(restDocumentation);

        var user = signup(mockMvc);
        Map<String, Object> loginRes = login(mockMvc, user.email());
        String refreshToken = (String) loginRes.get("refreshToken");

        var refreshReq = new HashMap<String, Object>();
        refreshReq.put("refreshToken", refreshToken);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andDo(document("auth-refresh",
                        requestFields(
                                fieldWithPath("refreshToken").type(STRING).description("Refresh Token (opaque)")
                        ),
                        responseFields(
                                fieldWithPath("accessToken").type(STRING).description("새 Access Token (JWT)"),
                                fieldWithPath("refreshToken").type(STRING).description("새 Refresh Token (rotation)"),
                                fieldWithPath("tokenType").type(STRING).description("토큰 타입 (Bearer)")
                        )
                ));
    }

    @Test
    void 토큰재사용_API_실패_401(RestDocumentationContextProvider restDocumentation) throws Exception {
        MockMvc mockMvc = mockMvc(restDocumentation);

        var user = signup(mockMvc);

        Map<String, Object> loginRes = login(mockMvc, user.email());
        String refreshA = (String) loginRes.get("refreshToken");

        // refresh(A) 1회 성공 -> A는 revoke 됨
        var refreshReqA = new HashMap<String, Object>();
        refreshReqA.put("refreshToken", refreshA);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshReqA)))
                .andExpect(status().isOk());

        // reuse(A) -> 401
        var reuseReq = new HashMap<String, Object>();
        reuseReq.put("refreshToken", refreshA);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reuseReq)))
                .andExpect(status().isUnauthorized())
                .andDo(document("auth-refresh-401-reuse",
                        responseFields(ErrorResponseSnippet.common())
                ));
    }

    @Test
    void 로그아웃올_API_성공(RestDocumentationContextProvider restDocumentation) throws Exception {
        MockMvc mockMvc = mockMvc(restDocumentation);

        // ✅ 로그인/회원가입 흐름에 의존하지 않게, 토큰 직접 발급
        var user = docs.saveUser("logoutall", "테스트유저");
        String accessToken = docs.issueTokenFor(user);

        mockMvc.perform(post("/api/auth/logout-all")
                        .header(DocsTestSupport.headerName(), DocsTestSupport.auth(accessToken)))
                .andExpect(status().isOk())
                .andDo(document("auth-logout-all",
                        requestHeaders(
                                headerWithName(DocsTestSupport.headerName())
                                        .description("Bearer Access Token")
                        ),
                        // ✅ response-fields.adoc 생성용
                        relaxedResponseFields(
                                fieldWithPath("accessToken").optional().type(STRING).description("응답에서는 null일 수 있음"),
                                fieldWithPath("refreshToken").optional().type(STRING).description("응답에서는 null일 수 있음"),
                                fieldWithPath("tokenType").optional().type(STRING).description("토큰 타입(Bearer)")
                        )
                ));
    }
}