package com.example.backend.controller.user;

import com.example.backend.domain.user.User;
import com.example.backend.repository.user.UserRepository;
import com.example.backend.security.JwtTokenProvider;
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
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ExtendWith({RestDocumentationExtension.class, SpringExtension.class})
@Transactional
class MeControllerDocsTest {

    @Autowired private WebApplicationContext context;

    @Autowired private UserRepository userRepository;
    @Autowired private JwtTokenProvider jwtTokenProvider;

    @Test
    void 내정보_조회_실패_토큰없음_401(RestDocumentationContextProvider restDocumentation) throws Exception {

        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity()) // ✅ Security 필터 체인 적용(핵심)
                .apply(documentationConfiguration(restDocumentation))
                .build();

        mockMvc.perform(get("/api/me"))
                .andExpect(status().isUnauthorized())
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andDo(document("me-401-unauthorized"));
    }

    @Test
    void 내정보_조회_성공_토큰있음_200(RestDocumentationContextProvider restDocumentation) throws Exception {

        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity()) // ✅ Security 필터 체인 적용(핵심)
                .apply(documentationConfiguration(restDocumentation))
                .build();

        String email = "me+" + java.util.UUID.randomUUID() + "@test.com";

        User user = userRepository.saveAndFlush(new User(
                email,
                "encoded",
                "시드유저"
        ));

        String token = jwtTokenProvider.createAccessToken(user.getId().toString(), user.getEmail());

        mockMvc.perform(get("/api/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andDo(document("me-200-ok",
                        requestHeaders(
                                headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer {accessToken}")
                        ),
                        responseFields(
                                fieldWithPath("email").description("현재 인증된 사용자 이메일")
                        )
                ));
    }
}
