package com.example.backend.controller.post;

import com.example.backend.domain.post.PostVisibility;
import com.example.backend.domain.user.User;
import com.example.backend.repository.user.UserRepository;
import com.example.backend.security.JwtTokenProvider;
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
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.UUID;

import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ExtendWith({RestDocumentationExtension.class, SpringExtension.class})
@Transactional
class PostControllerDocsTest {

    @Autowired private WebApplicationContext context;
    @Autowired private UserRepository userRepository;
    @Autowired private JwtTokenProvider jwtTokenProvider;
    @Autowired private ObjectMapper objectMapper;

    @Test
    void 게시글_생성_성공_201(RestDocumentationContextProvider restDocumentation) throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .apply(documentationConfiguration(restDocumentation))
                .build();

        String email = "post+" + UUID.randomUUID() + "@test.com";
        User user = userRepository.saveAndFlush(new User(email, "encoded", "게시글유저"));

        String token = jwtTokenProvider.createAccessToken(user.getId().toString(), user.getEmail());

        var req = new java.util.HashMap<String, Object>();
        req.put("content", "오늘은 진짜 리얼한 하루였다.");
        req.put("imageUrls", List.of(
                "https://example.com/images/1.jpg",
                "https://example.com/images/2.jpg"
        ));
        req.put("visibility", PostVisibility.ALL.name());

        mockMvc.perform(post("/api/posts")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.postId").exists())
                .andExpect(jsonPath("$.authorId").value(user.getId().toString()))
                .andExpect(jsonPath("$.content").value("오늘은 진짜 리얼한 하루였다."))
                .andExpect(jsonPath("$.imageUrls").isArray())
                .andExpect(jsonPath("$.visibility").value("ALL"))
                .andDo(document("posts-create",
                        requestHeaders(
                                headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer {accessToken}")
                        ),
                        requestFields(
                                fieldWithPath("content").description("게시글 본문"),
                                fieldWithPath("imageUrls").description("이미지 URL 목록(선택)").optional(),
                                fieldWithPath("visibility").description("공개 범위 (ALL/FOLLOWERS/PRIVATE)")
                        ),
                        responseFields(
                                fieldWithPath("postId").description("게시글 ID"),
                                fieldWithPath("authorId").description("작성자 ID"),
                                fieldWithPath("content").description("게시글 본문"),
                                fieldWithPath("imageUrls").description("이미지 URL 목록"),
                                fieldWithPath("visibility").description("공개 범위"),
                                fieldWithPath("createdAt").description("생성 시각")
                        )
                ));
    }

    @Test
    void 게시글_상세조회_성공_200(RestDocumentationContextProvider restDocumentation) throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .apply(documentationConfiguration(restDocumentation))
                .build();

        String email = "detail+" + UUID.randomUUID() + "@test.com";
        User user = userRepository.saveAndFlush(new User(email, "encoded", "상세유저"));
        String token = jwtTokenProvider.createAccessToken(user.getId().toString(), user.getEmail());

        // 게시글 생성(서비스/레포 사용해도 되지만, 빠르게는 create API를 호출해도 됨)
        var req = new java.util.HashMap<String, Object>();
        req.put("content", "상세 조회용 게시글");
        req.put("imageUrls", List.of("https://example.com/images/a.jpg"));
        req.put("visibility", PostVisibility.ALL.name());

        String createResult = mockMvc.perform(post("/api/posts")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // 응답에서 postId 추출
        String postId = objectMapper.readTree(createResult).get("postId").asText();

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/posts/{postId}", postId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.postId").value(postId))
                .andExpect(jsonPath("$.content").value("상세 조회용 게시글"))
                .andDo(document("posts-get",
                        requestHeaders(
                                headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer {accessToken}")
                        ),
                        pathParameters(
                                parameterWithName("postId").description("게시글 ID")
                        ),
                        responseFields(
                                fieldWithPath("postId").description("게시글 ID"),
                                fieldWithPath("authorId").description("작성자 ID"),
                                fieldWithPath("content").description("게시글 본문"),
                                fieldWithPath("imageUrls").description("이미지 URL 목록"),
                                fieldWithPath("visibility").description("공개 범위"),
                                fieldWithPath("createdAt").description("생성 시각")
                        )
                ));
    }

    @Test
    void 피드_조회_성공_200(RestDocumentationContextProvider restDocumentation) throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .apply(documentationConfiguration(restDocumentation))
                .build();

        String email = "feed+" + UUID.randomUUID() + "@test.com";
        User user = userRepository.saveAndFlush(new User(email, "encoded", "피드유저"));
        String token = jwtTokenProvider.createAccessToken(user.getId().toString(), user.getEmail());

        // 게시글 3개 생성
        for (int i = 1; i <= 3; i++) {
            var req = new java.util.HashMap<String, Object>();
            req.put("content", "피드용 게시글 " + i);
            req.put("imageUrls", List.of("https://example.com/images/" + i + ".jpg"));
            req.put("visibility", PostVisibility.ALL.name());

            mockMvc.perform(post("/api/posts")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated());
        }

        // size=2로 피드 조회
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/posts")
                        .param("size", "2")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.hasNext").value(true))
                .andExpect(jsonPath("$.nextCursor").exists())
                .andDo(document("posts-feed",
                        requestHeaders(
                                headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer {accessToken}")
                        ),
                        queryParameters(
                                parameterWithName("cursor").optional().description("다음 페이지 커서 (응답 nextCursor 값을 그대로 전달)"),
                                parameterWithName("size").optional().description("페이지 크기 (기본 20, 최대 50)")
                        ),
                        responseFields(
                                fieldWithPath("items").description("피드 게시글 목록(최신순)"),
                                fieldWithPath("items[].postId").description("게시글 ID"),
                                fieldWithPath("items[].authorId").description("작성자 ID"),
                                fieldWithPath("items[].content").description("게시글 본문"),
                                fieldWithPath("items[].imageUrls").description("이미지 URL 목록"),
                                fieldWithPath("items[].visibility").description("공개 범위"),
                                fieldWithPath("items[].createdAt").description("생성 시각"),
                                fieldWithPath("nextCursor").optional().description("다음 페이지 커서 (없으면 null)"),
                                fieldWithPath("hasNext").description("다음 페이지 존재 여부")
                        )
                ));
    }


}
