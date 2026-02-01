package com.example.backend.controller.postlike;

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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.HashMap;
import java.util.UUID;

import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

@SpringBootTest
@ExtendWith({RestDocumentationExtension.class, SpringExtension.class})
@Transactional
class PostLikeControllerDocsTest {

    @Autowired private WebApplicationContext context;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private JwtTokenProvider jwtTokenProvider;

    private MockMvc mockMvc(RestDocumentationContextProvider restDocumentation) {
        return MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .apply(documentationConfiguration(restDocumentation))
                .build();
    }

    private User saveUser(String prefix, String name) {
        String email = prefix + "+" + UUID.randomUUID() + "@test.com";
        String handle = prefix + "_" + UUID.randomUUID().toString().substring(0, 8);
        return userRepository.saveAndFlush(new User(email, handle, "encoded", name));
    }

    private String bearer(User user) {
        String token = jwtTokenProvider.createAccessToken(user.getId().toString(), user.getEmail());
        return "Bearer " + token;
    }

    private String createPostAndGetPostId(MockMvc mockMvc, String bearerToken) throws Exception {
        var req = new HashMap<String, Object>();
        req.put("content", "좋아요 테스트 게시글");
        req.put("imageUrls", java.util.List.of("https://example.com/image1.jpg"));
        req.put("visibility", "FOLLOWERS"); // 네 실제 응답과 동일

        MvcResult result = mockMvc.perform(post("/api/posts")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        String json = result.getResponse().getContentAsString();
        return objectMapper.readTree(json).get("postId").asText();
    }

    @Test
    void 좋아요_성공_204(RestDocumentationContextProvider restDocumentation) throws Exception {
        MockMvc mockMvc = mockMvc(restDocumentation);

        User me = saveUser("like", "좋아요유저");
        String token = bearer(me);

        String postId = createPostAndGetPostId(mockMvc, token);

        mockMvc.perform(post("/api/posts/{postId}/likes", postId)
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isNoContent())
                .andDo(document("posts-likes-create-204",
                        requestHeaders(
                                headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer {accessToken}")
                        ),
                        pathParameters(
                                parameterWithName("postId").description("좋아요할 게시글 ID(UUID)")
                        )
                ));
    }

    @Test
    void 좋아요_취소_성공_204(RestDocumentationContextProvider restDocumentation) throws Exception {
        MockMvc mockMvc = mockMvc(restDocumentation);

        User me = saveUser("unlike", "취소유저");
        String token = bearer(me);

        String postId = createPostAndGetPostId(mockMvc, token);

        // 먼저 좋아요
        mockMvc.perform(post("/api/posts/{postId}/likes", postId)
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isNoContent());

        // 좋아요 취소 (⚠️ 네 프로젝트가 DELETE가 아니라면 여기만 바꾸면 됨)
        mockMvc.perform(delete("/api/posts/{postId}/likes", postId)
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isNoContent())
                .andDo(document("posts-likes-delete-204",
                        requestHeaders(
                                headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer {accessToken}")
                        ),
                        pathParameters(
                                parameterWithName("postId").description("좋아요 취소할 게시글 ID(UUID)")
                        )
                ));
    }
}
