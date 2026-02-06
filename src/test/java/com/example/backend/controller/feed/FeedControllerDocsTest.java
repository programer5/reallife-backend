package com.example.backend.controller.feed;

import com.example.backend.controller.DocsTestSupport;
import com.example.backend.domain.follow.Follow;
import com.example.backend.repository.follow.FollowRepository;
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

    private MockMvc mockMvc(RestDocumentationContextProvider restDocumentation) {
        return MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .apply(documentationConfiguration(restDocumentation))
                .build();
    }

    @Test
    void 피드_조회_성공_200(RestDocumentationContextProvider restDocumentation) throws Exception {
        var me = docs.saveUser("feedme", "피드유저");
        String token = docs.issueTokenFor(me);

        var target = docs.saveUser("feedtarget", "타겟유저");

        // me가 target을 팔로우
        followRepository.saveAndFlush(Follow.create(me.getId(), target.getId()));

        // target 글 1개
        docs.savePost(target.getId(), "팔로잉 글");

        mockMvc(restDocumentation)
                .perform(get("/api/feed")
                        .param("cursor", "")   // ✅ 첫 페이지면 비워도 되고, 아예 param을 빼도 됨
                        .param("size", "20")   // ✅ 기본 20, 네 API에 맞게
                        .header(HttpHeaders.AUTHORIZATION, DocsTestSupport.auth(token)))
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andDo(document("feed-get",
                        requestHeaders(
                                headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer {accessToken}")
                        ),
                        queryParameters(
                                parameterWithName("cursor").optional().description("다음 페이지 커서(없으면 첫 페이지)"),
                                parameterWithName("size").optional().description("페이지 크기(기본값/최대값은 서버 정책)")
                        ),
                        responseFields(
                                fieldWithPath("items").type(ARRAY).description("피드 아이템 목록"),
                                fieldWithPath("items[].postId").type(STRING).description("게시글 ID"),
                                fieldWithPath("items[].authorId").type(STRING).description("작성자 ID"),
                                fieldWithPath("items[].authorHandle").type(STRING).description("작성자 핸들"),
                                fieldWithPath("items[].authorName").type(STRING).description("작성자 이름"),
                                fieldWithPath("items[].content").type(STRING).description("내용"),
                                fieldWithPath("items[].imageUrls").type(ARRAY).description("이미지 URL 목록"),
                                fieldWithPath("items[].visibility").type(STRING).description("공개범위"),
                                fieldWithPath("items[].createdAt").type(STRING).description("생성시각"),
                                fieldWithPath("nextCursor").optional().description("다음 페이지 커서(없으면 null)"),
                                fieldWithPath("hasNext").type(BOOLEAN).description("다음 페이지 존재 여부")
                        )
                ));
    }
}
