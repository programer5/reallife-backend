package com.example.backend.controller.user;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.UUID;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false) // ✅ Security 필터 비활성화(핵심)
@ExtendWith({RestDocumentationExtension.class, SpringExtension.class})
@Transactional
class UserControllerTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void 회원가입_API_성공(RestDocumentationContextProvider restDocumentation) throws Exception {

        this.mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(documentationConfiguration(restDocumentation))
                .build();

        String email = "test+" + UUID.randomUUID() + "@test.com";

        var request = new HashMap<>();
        request.put("email", email);
        request.put("password", "password1234");
        request.put("name", "테스트유저");

        String body = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/api/users")
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.name").value("테스트유저"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andDo(document("users-create",
                        requestFields(
                                fieldWithPath("email").description("사용자 이메일(로그인 ID)"),
                                fieldWithPath("password").description("비밀번호(최소 8자). 서버에 BCrypt로 암호화되어 저장됨"),
                                fieldWithPath("name").description("사용자 이름")
                        ),
                        responseFields(
                                fieldWithPath("id").description("사용자 식별자(UUID)"),
                                fieldWithPath("email").description("사용자 이메일"),
                                fieldWithPath("name").description("사용자 이름"),
                                fieldWithPath("createdAt").description("생성일시")
                        )
                ));
    }

    @Test
    void 회원가입_API_실패_검증오류_400(RestDocumentationContextProvider restDocumentation) throws Exception {

        this.mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(documentationConfiguration(restDocumentation))
                .build();

        // email 누락 -> 400
        var req = new java.util.HashMap<String, Object>();
        req.put("password", "password1234");
        req.put("name", "테스트유저");

        String body = objectMapper.writeValueAsString(req);

        mockMvc.perform(post("/api/users")
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.errors").isArray())
                .andDo(document("users-create-400-validation",
                        responseFields(
                                fieldWithPath("code").description("에러 코드"),
                                fieldWithPath("message").description("에러 메시지"),
                                fieldWithPath("timestamp").description("에러 발생 시각"),
                                fieldWithPath("errors").description("필드 오류 목록"),
                                fieldWithPath("errors[].field").description("오류가 발생한 필드명"),
                                fieldWithPath("errors[].reason").description("오류 사유")
                        )
                ));
    }

    @Test
    void 회원가입_API_실패_이메일중복_409(RestDocumentationContextProvider restDocumentation) throws Exception {

        this.mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(documentationConfiguration(restDocumentation))
                .build();

        String email = "dup@test.com";

        var req = new java.util.HashMap<String, Object>();
        req.put("email", email);
        req.put("password", "password1234");
        req.put("name", "테스트유저");

        String body = objectMapper.writeValueAsString(req);

        // 1번은 성공
        mockMvc.perform(post("/api/users")
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        // 2번은 중복으로 409
        mockMvc.perform(post("/api/users")
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_EMAIL"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists())
                .andDo(document("users-create-409-duplicate-email",
                        responseFields(
                                fieldWithPath("code").description("에러 코드"),
                                fieldWithPath("message").description("에러 메시지"),
                                fieldWithPath("timestamp").description("에러 발생 시각"),
                                fieldWithPath("errors").description("필드 오류 목록(없으면 빈 배열)")
                        )
                ));
    }
}
