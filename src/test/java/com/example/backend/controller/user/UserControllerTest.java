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
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.HashMap;
import java.util.UUID;

import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
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

        var req = new HashMap<String, Object>();
        req.put("email", email);
        req.put("password", "password1234");
        req.put("name", "테스트유저");

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk()) // ✅ 너 기존 프로젝트 흐름(회원가입 200) 기준
                .andExpect(jsonPath("$.email").value(email))
                .andDo(document("users-create",
                        requestFields(
                                fieldWithPath("email").description("가입 이메일"),
                                fieldWithPath("password").description("비밀번호(BCrypt 저장)"),
                                fieldWithPath("name").description("이름")
                        ),
                        responseFields(
                                fieldWithPath("id").description("사용자 ID(UUID)"),
                                fieldWithPath("email").description("가입 이메일"),
                                fieldWithPath("name").description("이름"),
                                fieldWithPath("createdAt").optional().description("생성일시")
                        )
                ));
    }

    @Test
    void 회원가입_API_실패_검증오류_400(RestDocumentationContextProvider restDocumentation) throws Exception {
        MockMvc mockMvc = mockMvc(restDocumentation);

        var req = new HashMap<String, Object>();
        req.put("email", "not-email");      // ❌ invalid
        req.put("password", "1");          // ❌ too short (가정)
        req.put("name", "");               // ❌ blank

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.path").value("/api/users"))
                .andDo(document("users-create-400-validation",
                        relaxedResponseFields(
                                fieldWithPath("code").description("에러 코드"),
                                fieldWithPath("message").description("에러 메시지"),
                                fieldWithPath("timestamp").description("에러 발생 시각"),
                                fieldWithPath("path").description("요청 경로"),
                                fieldWithPath("fieldErrors").optional().description("필드 검증 오류 목록 (검증 오류일 때만 존재)"),
                                fieldWithPath("fieldErrors[].field").optional().description("오류가 발생한 필드명"),
                                fieldWithPath("fieldErrors[].reason").optional().description("오류 사유")
                        )
                ));
    }

    @Test
    void 회원가입_API_실패_이메일중복_409(RestDocumentationContextProvider restDocumentation) throws Exception {
        MockMvc mockMvc = mockMvc(restDocumentation);

        String email = "dup+" + UUID.randomUUID() + "@test.com";

        var req = new HashMap<String, Object>();
        req.put("email", email);
        req.put("password", "password1234");
        req.put("name", "중복테스트");

        // 1) 첫 가입 성공
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        // 2) 두 번째 가입 → 409
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("USER_DUPLICATE_EMAIL"))
                .andDo(document("users-create-409-duplicate-email",
                        responseFields(ErrorResponseSnippet.common())
                ));
    }
}
