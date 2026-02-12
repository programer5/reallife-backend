package com.example.backend.controller.user;

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
import java.util.UUID;

import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.payload.JsonFieldType.ARRAY;
import static org.springframework.restdocs.payload.JsonFieldType.STRING;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@ExtendWith({RestDocumentationExtension.class, SpringExtension.class})
@Transactional
class UserControllerTest {

    @Autowired private WebApplicationContext context;
    @Autowired private ObjectMapper objectMapper;

    private MockMvc mockMvc(RestDocumentationContextProvider restDocumentation) {
        return MockMvcBuilders.webAppContextSetup(context)
                .apply(documentationConfiguration(restDocumentation))
                .build();
    }

    @Test
    void 회원가입_API_성공(RestDocumentationContextProvider restDocumentation) throws Exception {
        MockMvc mockMvc = mockMvc(restDocumentation);

        String email = "signup+" + UUID.randomUUID() + "@test.com";
        String handle = "user_" + UUID.randomUUID().toString().substring(0, 8);

        var req = new HashMap<String, Object>();
        req.put("email", email);
        req.put("handle", handle);
        req.put("password", "password1234");
        req.put("name", "테스트유저");

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk()) // 프로젝트 기준(200)
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.handle").value(handle))
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andDo(document("users-create",
                        requestFields(
                                fieldWithPath("email").type(STRING).description("가입 이메일(로그인에 사용)"),
                                fieldWithPath("handle").type(STRING).description("사용자 아이디(handle, 노출용 고유값)"),
                                fieldWithPath("password").type(STRING).description("비밀번호(BCrypt 저장)"),
                                fieldWithPath("name").type(STRING).description("이름")
                        ),
                        // ✅ 응답은 프로젝트 구현에 따라 달라질 수 있어 optional로 둠
                        relaxedResponseFields(
                                fieldWithPath("id").optional().type(STRING).description("사용자 ID(UUID)"),
                                fieldWithPath("email").type(STRING).description("가입 이메일"),
                                fieldWithPath("handle").type(STRING).description("사용자 아이디(handle)"),
                                fieldWithPath("name").type(STRING).description("이름")
                        )
                ));
    }

    @Test
    void 회원가입_API_실패_검증오류_400(RestDocumentationContextProvider restDocumentation) throws Exception {
        MockMvc mockMvc = mockMvc(restDocumentation);

        // ✅ 네가 실제로 받은 validation 에러 형태를 그대로 재현
        var req = new HashMap<String, Object>();
        req.put("email", "seed@test.com");
        req.put("password", "password1234");
        req.put("name", "시드유저");
        // handle intentionally missing

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_INVALID_REQUEST"))
                .andExpect(jsonPath("$.path").value("/api/users"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("handle"))
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andDo(document("users-create-400-validation",
                        relaxedResponseFields(
                                fieldWithPath("code").type(STRING).description("에러 코드"),
                                fieldWithPath("message").type(STRING).description("에러 메시지"),
                                fieldWithPath("timestamp").type(STRING).description("에러 발생 시각"),
                                fieldWithPath("path").type(STRING).description("요청 경로"),
                                fieldWithPath("fieldErrors").optional().type(ARRAY).description("필드 검증 오류 목록"),
                                fieldWithPath("fieldErrors[].field").optional().type(STRING).description("오류 필드"),
                                fieldWithPath("fieldErrors[].reason").optional().type(STRING).description("오류 사유")
                        )
                ));
    }

    @Test
    void 회원가입_API_실패_이메일중복_409(RestDocumentationContextProvider restDocumentation) throws Exception {
        MockMvc mockMvc = mockMvc(restDocumentation);

        String email = "dup+" + UUID.randomUUID() + "@test.com";
        String handle1 = "dup_" + UUID.randomUUID().toString().substring(0, 8);
        String handle2 = "dup_" + UUID.randomUUID().toString().substring(0, 8);

        var req1 = new HashMap<String, Object>();
        req1.put("email", email);
        req1.put("handle", handle1);
        req1.put("password", "password1234");
        req1.put("name", "중복테스트");

        // 1) 첫 가입 성공
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isOk());

        // 2) 같은 이메일로 다시 가입 → 409
        var req2 = new HashMap<String, Object>();
        req2.put("email", email);
        req2.put("handle", handle2);
        req2.put("password", "password1234");
        req2.put("name", "중복테스트2");

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("USER_DUPLICATE_EMAIL"))
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andDo(document("users-create-409-duplicate-email",
                        responseFields(ErrorResponseSnippet.common())
                ));
    }

    @Test
    void 회원가입_API_실패_아이디중복_409(RestDocumentationContextProvider restDocumentation) throws Exception {
        MockMvc mockMvc = mockMvc(restDocumentation);

        String handle = "same_" + UUID.randomUUID().toString().substring(0, 8);

        var req1 = new HashMap<String, Object>();
        req1.put("email", "h1+" + UUID.randomUUID() + "@test.com");
        req1.put("handle", handle);
        req1.put("password", "password1234");
        req1.put("name", "아이디중복1");

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isOk());

        var req2 = new HashMap<String, Object>();
        req2.put("email", "h2+" + UUID.randomUUID() + "@test.com");
        req2.put("handle", handle);
        req2.put("password", "password1234");
        req2.put("name", "아이디중복2");

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("USER_DUPLICATE_HANDLE"))
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andDo(document("users-create-409-duplicate-handle",
                        responseFields(ErrorResponseSnippet.common())
                ));
    }
}
