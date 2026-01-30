package com.example.backend.controller.follow;

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
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ExtendWith({RestDocumentationExtension.class, SpringExtension.class})
@Transactional
class FollowControllerDocsTest {

    @Autowired private WebApplicationContext context;
    @Autowired private UserRepository userRepository;
    @Autowired private JwtTokenProvider jwtTokenProvider;
    @Autowired private ObjectMapper objectMapper;

    @Test
    void 팔로우_성공_204(RestDocumentationContextProvider restDocumentation) throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .apply(documentationConfiguration(restDocumentation))
                .build();

        String email = "follow+" + UUID.randomUUID() + "@test.com";
        User me = userRepository.saveAndFlush(new User(email, "encoded", "팔로워"));
        User target = userRepository.saveAndFlush(new User("target+" + UUID.randomUUID() + "@test.com", "encoded", "타겟"));

        String token = jwtTokenProvider.createAccessToken(me.getId().toString(), me.getEmail());

        mockMvc.perform(post("/api/follows/{targetUserId}", target.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNoContent())
                .andDo(document("follows-create",
                        requestHeaders(headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer {accessToken}")),
                        pathParameters(parameterWithName("targetUserId").description("팔로우할 사용자 ID"))
                ));
    }

    @Test
    void 언팔로우_성공_204(RestDocumentationContextProvider restDocumentation) throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .apply(documentationConfiguration(restDocumentation))
                .build();

        String email = "unfollow+" + UUID.randomUUID() + "@test.com";
        User me = userRepository.saveAndFlush(new User(email, "encoded", "언팔로워"));
        User target = userRepository.saveAndFlush(new User("target2+" + UUID.randomUUID() + "@test.com", "encoded", "타겟2"));

        String token = jwtTokenProvider.createAccessToken(me.getId().toString(), me.getEmail());

        // 팔로우 후 언팔로우
        mockMvc.perform(post("/api/follows/{targetUserId}", target.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/follows/{targetUserId}", target.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNoContent())
                .andDo(document("follows-delete",
                        requestHeaders(headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer {accessToken}")),
                        pathParameters(parameterWithName("targetUserId").description("언팔로우할 사용자 ID"))
                ));
    }
}