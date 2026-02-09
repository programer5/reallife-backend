package com.example.backend.controller.feed;

import com.example.backend.controller.DocsTestSupport;
import com.example.backend.domain.follow.Follow;
import com.example.backend.repository.follow.FollowRepository;
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

import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.JsonFieldType.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ExtendWith({RestDocumentationExtension.class, SpringExtension.class})
@Transactional
class FeedControllerDocsTest {

    @Autowired WebApplicationContext context;
    @Autowired DocsTestSupport docs;
    @Autowired FollowRepository followRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private MockMvc mockMvc(RestDocumentationContextProvider restDocumentation) {
        return MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .apply(documentationConfiguration(restDocumentation))
                .build();
    }

    @Test
    void 피드_조회_첫페이지_성공_200(RestDocumentationContextProvider restDocumentation) throws Exception {
        var me = docs.saveUser("feedme", "피드유저");
        String token = docs.issueTokenFor(me);

        var target = docs.saveUser("feedtarget", "타겟유저");

        // me가 target을 팔로우
        followRepository.saveAndFlush(Follow.create(me.getId(), target.getId()));

        // target 글 여러 개(다음 페이지 테스트를 위해 3개)
        docs.savePost(target.getId(), "팔로잉 글 1");
        docs.savePost(target.getId(), "팔로잉 글 2");
        docs.savePost(target.getId(), "팔로잉 글 3");

        mockMvc(restDocumentation)
                .perform(get("/api/feed")
                        .param("size", "2")
                        .header(HttpHeaders.AUTHORIZATION, DocsTestSupport.auth(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.hasNext").value(true))
                .andDo(document("feed-get",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(
                                headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer {accessToken}")
                        ),
                        queryParameters(
                                parameterWithName("cursor").optional().description("페이지 커서(createdAt|id). 없으면 첫 페이지"),
                                parameterWithName("size").optional().description("페이지 크기(기본 20, 최대 50)")
                        ),
                        responseFields(
                                fieldWithPath("items").type(ARRAY).description("피드 아이템 목록(최신순)"),
                                fieldWithPath("items[].postId").type(STRING).description("게시글 ID"),
                                fieldWithPath("items[].authorId").type(STRING).description("작성자 ID"),
                                fieldWithPath("items[].authorHandle").type(STRING).description("작성자 핸들"),
                                fieldWithPath("items[].authorName").type(STRING).description("작성자 이름"),
                                fieldWithPath("items[].content").type(STRING).description("내용"),
                                fieldWithPath("items[].imageUrls").type(ARRAY).description("이미지 URL 목록"),
                                fieldWithPath("items[].visibility").type(STRING).description("공개범위"),
                                fieldWithPath("items[].createdAt").type(STRING).description("생성시각"),
                                fieldWithPath("nextCursor").optional().type(STRING).description("다음 페이지 커서(없으면 null)"),
                                fieldWithPath("hasNext").type(BOOLEAN).description("다음 페이지 존재 여부")
                        )
                ));
    }

    @Test
    void 피드_조회_다음페이지_성공_200(RestDocumentationContextProvider restDocumentation) throws Exception {
        var me = docs.saveUser("feedme", "피드유저");
        String token = docs.issueTokenFor(me);

        var target = docs.saveUser("feedtarget", "타겟유저");

        followRepository.saveAndFlush(Follow.create(me.getId(), target.getId()));

        docs.savePost(target.getId(), "팔로잉 글 1");
        docs.savePost(target.getId(), "팔로잉 글 2");
        docs.savePost(target.getId(), "팔로잉 글 3");

        String first = mockMvc(restDocumentation)
                .perform(get("/api/feed")
                        .param("size", "2")
                        .header(HttpHeaders.AUTHORIZATION, DocsTestSupport.auth(token)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String nextCursor = objectMapper.readTree(first).get("nextCursor").asText();

        mockMvc(restDocumentation)
                .perform(get("/api/feed")
                        .param("size", "2")
                        .param("cursor", nextCursor)
                        .header(HttpHeaders.AUTHORIZATION, DocsTestSupport.auth(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andDo(document("feed-get-next-page",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(
                                headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer {accessToken}")
                        ),
                        queryParameters(
                                parameterWithName("cursor").optional().description("페이지 커서(createdAt|id)"),
                                parameterWithName("size").optional().description("페이지 크기(기본 20, 최대 50)")
                        ),
                        responseFields(
                                fieldWithPath("items").type(ARRAY).description("피드 아이템 목록(최신순)"),
                                fieldWithPath("items[].postId").type(STRING).description("게시글 ID"),
                                fieldWithPath("items[].authorId").type(STRING).description("작성자 ID"),
                                fieldWithPath("items[].authorHandle").type(STRING).description("작성자 핸들"),
                                fieldWithPath("items[].authorName").type(STRING).description("작성자 이름"),
                                fieldWithPath("items[].content").type(STRING).description("내용"),
                                fieldWithPath("items[].imageUrls").type(ARRAY).description("이미지 URL 목록"),
                                fieldWithPath("items[].visibility").type(STRING).description("공개범위"),
                                fieldWithPath("items[].createdAt").type(STRING).description("생성시각"),
                                fieldWithPath("nextCursor").optional().type(STRING).description("다음 페이지 커서(없으면 null)"),
                                fieldWithPath("hasNext").type(BOOLEAN).description("다음 페이지 존재 여부")
                        )
                ));
    }
}