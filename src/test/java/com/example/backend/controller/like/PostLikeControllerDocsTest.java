package com.example.backend.controller.like;

import com.example.backend.domain.post.PostVisibility;
import com.example.backend.domain.user.User;
import com.example.backend.repository.user.UserRepository;
import com.example.backend.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.HashMap;
import java.util.List;
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
class PostLikeControllerDocsTest {

    @Autowired private WebApplicationContext context;
    @Autowired private UserRepository userRepository;
    @Autowired private JwtTokenProvider jwtTokenProvider;
    @Autowired private ObjectMapper objectMapper;

    private MockMvc mockMvc(RestDocumentationContextProvider restDocumentation) {
        return MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .apply(documentationConfiguration(restDocumentation))
                .build();
    }

    private String createPost(MockMvc mockMvc, String token) throws Exception {
        var req = new HashMap<String, Object>();
        req.put("content", "좋아요 테스트 게시글");
        req.put("imageUrls", List.of("https://example.com/images/like.jpg"));
        req.put("visibility", PostVisibility.ALL.name());

        String json = mockMvc.perform(post("/api/posts")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(json).get("postId").asText();
    }

    @Test
    void 좋아요_성공_204(RestDocumentationContextProvider restDocumentation) throws Exception {
        MockMvc mockMvc = mockMvc(restDocumentation);

        User me = userRepository.saveAndFlush(new User("like+" + UUID.randomUUID() + "@test.com", "encoded", "좋아요유저"));
        String token = jwtTokenProvider.createAccessToken(me.getId().toString(), me.getEmail());

        String postId = createPost(mockMvc, token);

        mockMvc.perform(post("/api/posts/{postId}/likes", postId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNoContent())
                .andDo(document("post-likes-create",
                        requestHeaders(headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer {accessToken}")),
                        pathParameters(parameterWithName("postId").description("좋아요할 게시글 ID"))
                ));
    }

    @Test
    void 좋아요_취소_성공_204(RestDocumentationContextProvider restDocumentation) throws Exception {
        MockMvc mockMvc = mockMvc(restDocumentation);

        User me = userRepository.saveAndFlush(new User("unlike+" + UUID.randomUUID() + "@test.com", "encoded", "취소유저"));
        String token = jwtTokenProvider.createAccessToken(me.getId().toString(), me.getEmail());

        String postId = createPost(mockMvc, token);

        // 먼저 좋아요
        mockMvc.perform(post("/api/posts/{postId}/likes", postId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNoContent());

        // 취소
        mockMvc.perform(delete("/api/posts/{postId}/likes", postId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNoContent())
                .andDo(document("post-likes-delete",
                        requestHeaders(headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer {accessToken}")),
                        pathParameters(parameterWithName("postId").description("좋아요 취소할 게시글 ID"))
                ));
    }
}
