package com.example.backend.regression;

import com.example.backend.controller.DocsTestSupport;
import com.example.backend.domain.follow.Follow;
import com.example.backend.domain.post.Post;
import com.example.backend.domain.post.PostVisibility;
import com.example.backend.repository.follow.FollowRepository;
import com.example.backend.repository.post.PostRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
@Transactional
class AuthAndFeedRegressionTest {

    @Autowired WebApplicationContext context;
    @Autowired DocsTestSupport docs;
    @Autowired FollowRepository followRepository;
    @Autowired PostRepository postRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private MockMvc mockMvc() {
        return MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void 익명_핸들중복확인과_refresh_cookie_쿠키없음_401() throws Exception {
        MockMvc mockMvc = mockMvc();

        String handle = "exists_" + UUID.randomUUID().toString().substring(0, 8);

        mockMvc.perform(get("/api/users/exists").param("handle", handle))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exists").value(false));

        var req = new HashMap<String, Object>();
        req.put("email", "exists+" + UUID.randomUUID() + "@test.com");
        req.put("handle", handle);
        req.put("password", "password1234");
        req.put("name", "회귀테스트유저");

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/users/exists").param("handle", handle))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exists").value(true));

        mockMvc.perform(post("/api/auth/refresh-cookie"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_UNAUTHORIZED"));
    }

    @Test
    void 공개범위_피드_회귀_비팔로우와_팔로우규칙() throws Exception {
        MockMvc mockMvc = mockMvc();

        var me = docs.saveUser("viewer", "조회자");
        String token = docs.issueTokenFor(me);

        var stranger = docs.saveUser("stranger", "비팔로우작성자");
        var followed = docs.saveUser("followed", "팔로우작성자");

        postRepository.saveAndFlush(Post.create(stranger.getId(), "stranger all", PostVisibility.ALL));
        postRepository.saveAndFlush(Post.create(stranger.getId(), "stranger followers", PostVisibility.FOLLOWERS));
        postRepository.saveAndFlush(Post.create(stranger.getId(), "stranger private", PostVisibility.PRIVATE));

        postRepository.saveAndFlush(Post.create(followed.getId(), "followed all", PostVisibility.ALL));
        postRepository.saveAndFlush(Post.create(followed.getId(), "followed followers", PostVisibility.FOLLOWERS));
        postRepository.saveAndFlush(Post.create(followed.getId(), "followed private", PostVisibility.PRIVATE));

        followRepository.saveAndFlush(Follow.create(me.getId(), followed.getId()));

        MvcResult result = mockMvc.perform(get("/api/feed")
                        .param("size", "20")
                        .header(DocsTestSupport.headerName(), DocsTestSupport.auth(token)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        Set<String> contents = new HashSet<>();
        for (JsonNode item : root.path("items")) {
            contents.add(item.path("content").asText());
        }

        assertThat(contents).contains("stranger all");
        assertThat(contents).doesNotContain("stranger followers", "stranger private");
        assertThat(contents).contains("followed all", "followed followers");
        assertThat(contents).doesNotContain("followed private");
    }
}
