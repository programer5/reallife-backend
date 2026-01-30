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

import java.util.UUID;

import static org.springframework.restdocs.headers.HeaderDocumentation.*;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
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

    private MockMvc mockMvc(RestDocumentationContextProvider restDocumentation) {
        return MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .apply(documentationConfiguration(restDocumentation))
                .build();
    }

    @Test
    void 팔로우_성공_204(RestDocumentationContextProvider restDocumentation) throws Exception {
        MockMvc mockMvc = mockMvc(restDocumentation);

        User me = userRepository.saveAndFlush(new User("follow+" + UUID.randomUUID() + "@test.com", "encoded", "팔로워"));
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
        MockMvc mockMvc = mockMvc(restDocumentation);

        User me = userRepository.saveAndFlush(new User("unfollow+" + UUID.randomUUID() + "@test.com", "encoded", "언팔로워"));
        User target = userRepository.saveAndFlush(new User("target2+" + UUID.randomUUID() + "@test.com", "encoded", "타겟2"));

        String token = jwtTokenProvider.createAccessToken(me.getId().toString(), me.getEmail());

        // 먼저 팔로우
        mockMvc.perform(post("/api/follows/{targetUserId}", target.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNoContent());

        // 언팔로우
        mockMvc.perform(delete("/api/follows/{targetUserId}", target.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNoContent())
                .andDo(document("follows-delete",
                        requestHeaders(headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer {accessToken}")),
                        pathParameters(parameterWithName("targetUserId").description("언팔로우할 사용자 ID"))
                ));
    }
}
