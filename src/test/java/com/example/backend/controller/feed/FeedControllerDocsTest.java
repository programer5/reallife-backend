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
import org.springframework.test.context.ActiveProfiles;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
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
                                subsectionWithPath("items[].mediaItems").type(ARRAY).description("미디어 목록(이미지/동영상). 각 항목은 mediaType, url, thumbnailUrl, contentType을 포함"),
                                fieldWithPath("items[].visibility").type(STRING).description("공개범위"),
                                fieldWithPath("items[].createdAt").type(STRING).description("생성시각"),
                                fieldWithPath("items[].likeCount").type(NUMBER).description("좋아요 수"),
                                fieldWithPath("items[].commentCount").type(NUMBER).description("댓글 수"),
                                fieldWithPath("items[].likedByMe").type(BOOLEAN).description("내가 좋아요 했는지"),
                                fieldWithPath("items[].latitude").optional().type(VARIES).description("게시글 위치 위도. 위치가 없으면 null"),
                                fieldWithPath("items[].longitude").optional().type(VARIES).description("게시글 위치 경도. 위치가 없으면 null"),
                                fieldWithPath("items[].placeName").optional().type(VARIES).description("장소명. 위치/장소 정보가 없으면 null"),
                                fieldWithPath("items[].distanceKm").optional().type(VARIES).description("nearby 조회 기준 거리(km). 일반 피드에서는 null"),
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
                                subsectionWithPath("items[].mediaItems").type(ARRAY).description("미디어 목록(이미지/동영상). 각 항목은 mediaType, url, thumbnailUrl, contentType을 포함"),
                                fieldWithPath("items[].visibility").type(STRING).description("공개범위"),
                                fieldWithPath("items[].createdAt").type(STRING).description("생성시각"),
                                fieldWithPath("items[].likeCount").type(NUMBER).description("좋아요 수"),
                                fieldWithPath("items[].commentCount").type(NUMBER).description("댓글 수"),
                                fieldWithPath("items[].likedByMe").type(BOOLEAN).description("내가 좋아요 했는지"),
                                fieldWithPath("items[].latitude").optional().type(VARIES).description("게시글 위치 위도. 위치가 없으면 null"),
                                fieldWithPath("items[].longitude").optional().type(VARIES).description("게시글 위치 경도. 위치가 없으면 null"),
                                fieldWithPath("items[].placeName").optional().type(VARIES).description("장소명. 위치/장소 정보가 없으면 null"),
                                fieldWithPath("items[].distanceKm").optional().type(VARIES).description("nearby 조회 기준 거리(km). 일반 피드에서는 null"),
                                fieldWithPath("nextCursor").optional().type(STRING).description("다음 페이지 커서(없으면 null)"),
                                fieldWithPath("hasNext").type(BOOLEAN).description("다음 페이지 존재 여부")
                        )
                ));
    }


    @Test
    void 피드_조회_전체공개_비팔로우도_노출_200(RestDocumentationContextProvider restDocumentation) throws Exception {
        var me = docs.saveUser("feedviewer", "피드조회자");
        String token = docs.issueTokenFor(me);

        var stranger = docs.saveUser("feedpublic", "전체공개작성자");

        // 팔로우하지 않았지만 ALL 공개 글은 보여야 한다.
        docs.savePost(stranger.getId(), "전체공개 글 1");
        docs.savePost(stranger.getId(), "전체공개 글 2");

        mockMvc(restDocumentation)
                .perform(get("/api/feed")
                        .param("size", "10")
                        .header(HttpHeaders.AUTHORIZATION, DocsTestSupport.auth(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items[0].authorId").value(stranger.getId().toString()))
                .andExpect(jsonPath("$.items[0].visibility").value("ALL"))
                .andDo(document("feed-get-public-visible",
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
                                subsectionWithPath("items[].mediaItems").type(ARRAY).description("미디어 목록(이미지/동영상). 각 항목은 mediaType, url, thumbnailUrl, contentType을 포함"),
                                fieldWithPath("items[].visibility").type(STRING).description("공개범위"),
                                fieldWithPath("items[].createdAt").type(STRING).description("생성시각"),
                                fieldWithPath("items[].likeCount").type(NUMBER).description("좋아요 수"),
                                fieldWithPath("items[].commentCount").type(NUMBER).description("댓글 수"),
                                fieldWithPath("items[].likedByMe").type(BOOLEAN).description("내가 좋아요 했는지"),
                                fieldWithPath("items[].latitude").optional().type(VARIES).description("게시글 위치 위도. 위치가 없으면 null"),
                                fieldWithPath("items[].longitude").optional().type(VARIES).description("게시글 위치 경도. 위치가 없으면 null"),
                                fieldWithPath("items[].placeName").optional().type(VARIES).description("장소명. 위치/장소 정보가 없으면 null"),
                                fieldWithPath("items[].distanceKm").optional().type(VARIES).description("nearby 조회 기준 거리(km). 일반 피드에서는 null"),
                                fieldWithPath("nextCursor").optional().type(STRING).description("다음 페이지 커서(없으면 null)"),
                                fieldWithPath("hasNext").type(BOOLEAN).description("다음 페이지 존재 여부")
                        )
                ));
    }



    @Test
    void 피드_근처_조회_거리순_성공_200(RestDocumentationContextProvider restDocumentation) throws Exception {
        var me = docs.saveUser("nearbyme", "근처조회자");
        String token = docs.issueTokenFor(me);

        var nearbyAuthor = docs.saveUser("nearbywriter", "근처작성자");
        var farAuthor = docs.saveUser("farwriter", "먼작성자");

        docs.savePostWithLocation(nearbyAuthor.getId(), "근처 카페에서 작성한 글", 37.5665, 126.9780, "서울 시청 근처 카페");
        docs.savePostWithLocation(farAuthor.getId(), "조금 먼 장소에서 작성한 글", 37.5700, 126.9920, "종로 근처");

        mockMvc(restDocumentation)
                .perform(get("/api/feed/nearby")
                        .param("lat", "37.5665")
                        .param("lng", "126.9780")
                        .param("size", "10")
                        .header(HttpHeaders.AUTHORIZATION, DocsTestSupport.auth(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items[0].placeName").value("서울 시청 근처 카페"))
                .andExpect(jsonPath("$.items[0].distanceKm").value(0.0))
                .andDo(document("feed-nearby-get",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(
                                headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer {accessToken}")
                        ),
                        queryParameters(
                                parameterWithName("lat").description("현재 위치 위도"),
                                parameterWithName("lng").description("현재 위치 경도"),
                                parameterWithName("size").optional().description("조회 개수(기본 30, 최대 50)")
                        ),
                        responseFields(
                                fieldWithPath("items").type(ARRAY).description("근처 피드 아이템 목록(거리순)"),
                                fieldWithPath("items[].postId").type(STRING).description("게시글 ID"),
                                fieldWithPath("items[].authorId").type(STRING).description("작성자 ID"),
                                fieldWithPath("items[].authorHandle").type(STRING).description("작성자 핸들"),
                                fieldWithPath("items[].authorName").type(STRING).description("작성자 이름"),
                                fieldWithPath("items[].content").type(STRING).description("내용"),
                                fieldWithPath("items[].imageUrls").type(ARRAY).description("이미지 URL 목록"),
                                subsectionWithPath("items[].mediaItems").type(ARRAY).description("미디어 목록(이미지/동영상). 각 항목은 mediaType, url, thumbnailUrl, contentType을 포함"),
                                fieldWithPath("items[].visibility").type(STRING).description("공개범위"),
                                fieldWithPath("items[].createdAt").type(STRING).description("생성시각"),
                                fieldWithPath("items[].likeCount").type(NUMBER).description("좋아요 수"),
                                fieldWithPath("items[].commentCount").type(NUMBER).description("댓글 수"),
                                fieldWithPath("items[].likedByMe").type(BOOLEAN).description("내가 좋아요 했는지"),
                                fieldWithPath("items[].latitude").optional().type(NUMBER).description("게시글 위치 위도"),
                                fieldWithPath("items[].longitude").optional().type(NUMBER).description("게시글 위치 경도"),
                                fieldWithPath("items[].placeName").optional().type(STRING).description("장소명"),
                                fieldWithPath("items[].distanceKm").optional().type(NUMBER).description("현재 위치 기준 거리(km)"),
                                fieldWithPath("nextCursor").optional().type(STRING).description("근처 피드는 현재 cursor를 사용하지 않으므로 null"),
                                fieldWithPath("hasNext").type(BOOLEAN).description("근처 피드는 현재 단일 페이지 응답이므로 false")
                        )
                ));
    }

}
