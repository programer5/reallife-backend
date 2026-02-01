package com.example.backend.controller.follow;

import com.example.backend.domain.user.User;
import com.example.backend.repository.user.UserRepository;
import com.example.backend.security.JwtTokenProvider;
import com.example.backend.restdocs.ErrorResponseSnippet;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.util.UUID;

import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

@SpringBootTest
@ExtendWith({RestDocumentationExtension.class, SpringExtension.class})
@Transactional
class FollowControllerDocsTest {

    @Autowired private WebApplicationContext context;
    @Autowired private ObjectMapper objectMapper; // (필요 없지만 주입되어 있어도 무방)
    @Autowired private UserRepository userRepository;
    @Autowired private JwtTokenProvider jwtTokenProvider;

    private MockMvc mockMvc(RestDocumentationContextProvider restDocumentation) {
        return MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .apply(documentationConfiguration(restDocumentation))
                .build();
    }

    private User saveUser(String prefix, String name) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String email = prefix + "+" + suffix + "@t.com";
        String handle = (prefix + "_" + suffix);
        if (handle.length() > 20) handle = handle.substring(0, 20);
        return userRepository.saveAndFlush(new User(email, handle, "encoded", name));
    }

    private String bearer(User user) {
        String token = jwtTokenProvider.createAccessToken(user.getId().toString(), user.getEmail());
        return "Bearer " + token;
    }

    @Test
    void 팔로우_성공_204(RestDocumentationContextProvider restDocumentation) throws Exception {
        MockMvc mockMvc = mockMvc(restDocumentation);

        User me = saveUser("followme", "팔로워");
        User target = saveUser("followtarget", "타겟");

        mockMvc.perform(post("/api/follows/{targetUserId}", target.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(me)))
                .andExpect(status().isNoContent())
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
        MockMvc mockMvc = mockMvc(restDocumentation);

        User me = saveUser("self", "나");

        mockMvc.perform(post("/api/follows/{targetUserId}", me.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(me)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("FOLLOW_CANNOT_FOLLOW_SELF"))
                .andDo(document("follows-create-400-self",
                        requestHeaders(
                                headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer {accessToken}")
                        ),
                        pathParameters(
                                parameterWithName("targetUserId").description("팔로우 대상 사용자 ID(UUID)")
                        ),
                        // 공통 에러 응답 문서화
                        org.springframework.restdocs.payload.PayloadDocumentation.responseFields(ErrorResponseSnippet.common())
                ));
    }
}
