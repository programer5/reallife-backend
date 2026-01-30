package com.example.backend.controller.post;

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
import static org.springframework.restdocs.payload.JsonFieldType.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ExtendWith({RestDocumentationExtension.class, SpringExtension.class})
@Transactional
class PostControllerDocsTest {

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

    private String tokenOf(User user) {
        return jwtTokenProvider.createAccessToken(user.getId().toString(), user.getEmail());
    }

    private String createPost(MockMvc mockMvc, String token, String content) throws Exception {
        var req = new HashMap<String, Object>();
        req.put("content", content);
        req.put("imageUrls", List.of("https://example.com/images/p1.jpg"));
        req.put("visibility", PostVisibility.ALL.name());

        String json = mockMvc.perform(post("/api/posts")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        var node = objectMapper.readTree(json);
        if (node.hasNonNull("postId")) return node.get("postId").asText();
        if (node.hasNonNull("id")) return node.get("id").asText();
        throw new IllegalStateException("Post create response must contain postId or id: " + json);
    }

    @Test
    void 게시글_생성_성공_201(RestDocumentationContextProvider restDocumentation) throws Exception {
        MockMvc mockMvc = mockMvc(restDocumentation);

        User me = userRepository.saveAndFlush(new User("post+" + UUID.randomUUID() + "@test.com", "encoded", "작성자"));
        String token = tokenOf(me);

        var req = new HashMap<String, Object>();
        req.put("content", "오늘의 리얼한 삶");
        req.put("imageUrls", List.of("https://example.com/images/real.jpg"));
        req.put("visibility", PostVisibility.ALL.name());

        mockMvc.perform(post("/api/posts")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.postId").exists())
                .andDo(document("posts-create",
                        requestHeaders(headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer {accessToken}")),
                        relaxedRequestFields(
                                fieldWithPath("content").type(STRING).description("게시글 본문"),
                                fieldWithPath("imageUrls").optional().type(ARRAY).description("이미지 URL 목록"),
                                fieldWithPath("visibility").optional().type(STRING).description("공개 범위")
                        ),
                        relaxedResponseFields(
                                fieldWithPath("postId").optional().type(STRING).description("생성된 게시글 ID(postId)"),
                                fieldWithPath("id").optional().type(STRING).description("생성된 게시글 ID(id)")
                        )
                ));
    }

    @Test
    void 게시글_상세조회_성공_200(RestDocumentationContextProvider restDocumentation) throws Exception {
        MockMvc mockMvc = mockMvc(restDocumentation);

        User me = userRepository.saveAndFlush(new User("detail+" + UUID.randomUUID() + "@test.com", "encoded", "작성자"));
        String token = tokenOf(me);

        String postId = createPost(mockMvc, token, "상세 조회용 게시글");

        mockMvc.perform(get("/api/posts/{postId}", postId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.postId").value(postId))
                .andDo(document("posts-get",
                        requestHeaders(headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer {accessToken}")),
                        pathParameters(parameterWithName("postId").description("조회할 게시글 ID")),
                        relaxedResponseFields(
                                fieldWithPath("postId").optional().type(STRING).description("게시글 ID(postId)"),
                                fieldWithPath("id").optional().type(STRING).description("게시글 ID(id)"),
                                fieldWithPath("authorId").optional().type(STRING).description("작성자 ID"),
                                fieldWithPath("content").optional().type(STRING).description("본문"),
                                fieldWithPath("imageUrls").optional().type(ARRAY).description("이미지 URL 목록"),
                                fieldWithPath("visibility").optional().type(STRING).description("공개 범위"),
                                fieldWithPath("likeCount").optional().type(NUMBER).description("좋아요 수"),
                                fieldWithPath("createdAt").optional().type(STRING).description("생성 시각")
                        )
                ));
    }

    @Test
    void 게시글_삭제_성공_204(RestDocumentationContextProvider restDocumentation) throws Exception {
        MockMvc mockMvc = mockMvc(restDocumentation);

        User me = userRepository.saveAndFlush(new User("del+" + UUID.randomUUID() + "@test.com", "encoded", "작성자"));
        String token = tokenOf(me);

        String postId = createPost(mockMvc, token, "삭제될 게시글");

        mockMvc.perform(delete("/api/posts/{postId}", postId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNoContent())
                .andDo(document("posts-delete",
                        requestHeaders(headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer {accessToken}")),
                        pathParameters(parameterWithName("postId").description("삭제할 게시글 ID"))
                ));
    }

    @Test
    void 피드_조회_성공_200_팔로우기반(RestDocumentationContextProvider restDocumentation) throws Exception {
        MockMvc mockMvc = mockMvc(restDocumentation);

        // 나
        User me = userRepository.saveAndFlush(new User("feedme+" + UUID.randomUUID() + "@test.com", "encoded", "피드유저"));
        String myToken = tokenOf(me);

        // 타겟(내가 팔로우할 유저)
        User target = userRepository.saveAndFlush(new User("feedtarget+" + UUID.randomUUID() + "@test.com", "encoded", "타겟유저"));
        String targetToken = tokenOf(target);

        // 내가 타겟 팔로우
        mockMvc.perform(post("/api/follows/{targetUserId}", target.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + myToken))
                .andExpect(status().isNoContent());

        // 타겟이 게시글 3개 작성
        for (int i = 1; i <= 3; i++) {
            createPost(mockMvc, targetToken, "타겟의 리얼 게시글 " + i);
        }

        mockMvc.perform(get("/api/posts")
                        .param("size", "2")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + myToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].authorId").value(target.getId().toString()))
                .andExpect(jsonPath("$.hasNext").value(true))
                .andExpect(jsonPath("$.nextCursor").exists())
                .andDo(document("posts-feed",
                        requestHeaders(headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer {accessToken}")),
                        queryParameters(
                                parameterWithName("cursor").optional().description("다음 페이지 커서(nextCursor 값을 그대로 전달)"),
                                parameterWithName("size").optional().description("페이지 크기(기본 20, 최대 50)")
                        ),
                        responseFields(
                                fieldWithPath("items").description("피드 게시글 목록(최신순)"),
                                fieldWithPath("items[].postId").description("게시글 ID"),
                                fieldWithPath("items[].authorId").description("작성자 ID(나 + 팔로우한 사용자)"),
                                fieldWithPath("items[].content").description("본문"),
                                fieldWithPath("items[].imageUrls").description("이미지 URL 목록"),
                                fieldWithPath("items[].visibility").description("공개 범위"),
                                fieldWithPath("items[].createdAt").description("생성 시각"),
                                fieldWithPath("nextCursor").optional().description("다음 페이지 커서(없으면 null)"),
                                fieldWithPath("hasNext").description("다음 페이지 존재 여부")
                        )
                ));
    }
}
