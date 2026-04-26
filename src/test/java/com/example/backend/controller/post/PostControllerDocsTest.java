package com.example.backend.controller.post;

import com.example.backend.controller.DocsTestSupport;
import com.example.backend.domain.file.UploadedFile;
import com.example.backend.repository.file.UploadedFileRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.HashMap;

import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.JsonFieldType.ARRAY;
import static org.springframework.restdocs.payload.JsonFieldType.NUMBER;
import static org.springframework.restdocs.payload.JsonFieldType.STRING;
import static org.springframework.restdocs.payload.JsonFieldType.VARIES;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.snippet.Attributes.key;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@ActiveProfiles("test")
@ExtendWith({RestDocumentationExtension.class, SpringExtension.class})
@Transactional
class PostControllerDocsTest {

    @Autowired private WebApplicationContext context;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private DocsTestSupport docs;
    @Autowired private UploadedFileRepository uploadedFileRepository;

    private MockMvc mockMvc(RestDocumentationContextProvider restDocumentation) {
        return MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .apply(documentationConfiguration(restDocumentation))
                .build();
    }

    @Test
    void 게시글_생성_성공_201_imageFileIds(RestDocumentationContextProvider restDocumentation) throws Exception {
        MockMvc mockMvc = mockMvc(restDocumentation);

        var user = docs.saveUser("post", "작성자");
        String token = docs.issueTokenFor(user);

        // ✅ imageFileIds 문서화를 위해 UploadedFile 한 건 생성
        var uploaded = UploadedFile.create(
                user.getId(),
                "cat.png",
                "docs/cat.png",
                "image/png",
                10L
        );
        uploadedFileRepository.saveAndFlush(uploaded);

        var req = new HashMap<String, Object>();
        req.put("content", "팔로워에게만 공유합니다 🙂");
        req.put("imageUrls", java.util.List.of("https://example.com/legacy-image.jpg")); // 구버전 호환 문서용
        req.put("imageFileIds", java.util.List.of(uploaded.getId().toString()));        // ✅ 권장 방식
        req.put("visibility", "FOLLOWERS");
        req.put("latitude", 37.5665);
        req.put("longitude", 126.9780);
        req.put("placeName", "서울 시청 근처");

        mockMvc.perform(post("/api/posts")
                        .header(DocsTestSupport.headerName(), DocsTestSupport.auth(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.postId").exists())
                .andExpect(jsonPath("$.authorId").exists())
                .andExpect(jsonPath("$.authorHandle").value(user.getHandle()))
                .andExpect(jsonPath("$.authorName").value(user.getName()))
                .andExpect(jsonPath("$.visibility").value("FOLLOWERS"))
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andDo(document("posts-create",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(
                                headerWithName(DocsTestSupport.headerName()).description("Bearer {accessToken}")
                        ),
                        requestFields(
                                fieldWithPath("content").type(STRING).description("게시글 본문"),
                                fieldWithPath("imageUrls").optional().type(ARRAY).description("(구버전 호환) 이미지 URL 목록"),
                                fieldWithPath("imageFileIds").optional().type(ARRAY).description("(권장) 업로드된 이미지 파일 ID(UUID) 목록"),
                                fieldWithPath("mediaFileIds").optional().type(ARRAY).description("(권장) 업로드된 미디어 파일 ID(UUID) 목록. 이미지/동영상 모두 가능"),
                                fieldWithPath("latitude").optional().type(NUMBER).description("게시글 위치 위도. longitude와 함께 전달하면 근처 피드에 사용"),
                                fieldWithPath("longitude").optional().type(NUMBER).description("게시글 위치 경도. latitude와 함께 전달하면 근처 피드에 사용"),
                                fieldWithPath("placeName").optional().type(STRING).description("장소명 또는 위치 라벨"),
                                fieldWithPath("visibility").optional().type(STRING)
                                        .attributes(key("constraints").value("ALL | FOLLOWERS | PRIVATE"))
                                        .description("공개 범위")
                        ),
                        responseFields(
                                fieldWithPath("postId").type(STRING).description("생성된 게시글 ID(UUID)"),
                                fieldWithPath("authorId").type(STRING).description("작성자 ID(UUID)"),
                                fieldWithPath("authorHandle").type(STRING).description("작성자 handle"),
                                fieldWithPath("authorName").type(STRING).description("작성자 이름"),
                                fieldWithPath("content").type(STRING).description("게시글 본문"),
                                fieldWithPath("imageUrls").type(ARRAY).description("이미지 URL 목록(다운로드 URL로 반환될 수 있음)"),
                                subsectionWithPath("mediaItems").type(ARRAY).description("미디어 목록(이미지/동영상). 각 항목은 mediaType, url, thumbnailUrl, contentType을 포함"),
                                fieldWithPath("visibility").type(STRING).description("공개 범위"),
                                fieldWithPath("createdAt").type(STRING).description("생성 시각(ISO-8601)"),
                                fieldWithPath("likeCount").description("좋아요 수"),
                                fieldWithPath("commentCount").description("댓글 수"),
                                fieldWithPath("likedByMe").description("내가 좋아요 눌렀는지 여부"),
                                fieldWithPath("latitude").optional().type(VARIES).description("게시글 위치 위도. 위치가 없으면 null"),
                                fieldWithPath("longitude").optional().type(VARIES).description("게시글 위치 경도. 위치가 없으면 null"),
                                fieldWithPath("placeName").optional().type(VARIES).description("장소명. 위치/장소 정보가 없으면 null"),
                                fieldWithPath("distanceKm").optional().type(VARIES).description("nearby 조회 기준 거리(km). 일반 게시글 응답에서는 null")
                        )
                ));
    }

    @Test
    void 게시글_상세조회_성공_200(RestDocumentationContextProvider restDocumentation) throws Exception {
        MockMvc mockMvc = mockMvc(restDocumentation);

        var user = docs.saveUser("detail", "작성자");
        String token = docs.issueTokenFor(user);

        var createReq = new HashMap<String, Object>();
        createReq.put("content", "상세조회 테스트");
        createReq.put("imageUrls", java.util.List.of("https://example.com/image1.jpg"));
        createReq.put("visibility", "FOLLOWERS");

        MvcResult created = mockMvc.perform(post("/api/posts")
                        .header(DocsTestSupport.headerName(), DocsTestSupport.auth(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        String body = created.getResponse().getContentAsString();
        String postId = objectMapper.readTree(body).get("postId").asText();

        // ✅ 댓글 생성 후 상세 commentCount가 DB 기준으로 유지되는지 같이 검증
        var commentReq = new HashMap<String, Object>();
        commentReq.put("content", "댓글 1");

        mockMvc.perform(post("/api/posts/{postId}/comments", postId)
                        .header(DocsTestSupport.headerName(), DocsTestSupport.auth(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(commentReq)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/posts/{postId}", postId)
                .header(DocsTestSupport.headerName(), DocsTestSupport.auth(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authorHandle").value(user.getHandle()))
                .andExpect(jsonPath("$.authorName").value(user.getName()))
                .andExpect(jsonPath("$.likeCount").value(0))
                .andExpect(jsonPath("$.commentCount").value(1))
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andDo(document("posts-get",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(
                                headerWithName(DocsTestSupport.headerName()).description("Bearer {accessToken}")
                        ),
                        pathParameters(
                                parameterWithName("postId").description("조회할 게시글 ID(UUID)")
                        ),
                        responseFields(
                                fieldWithPath("postId").type(STRING).description("게시글 ID(UUID)"),
                                fieldWithPath("authorId").type(STRING).description("작성자 ID(UUID)"),
                                fieldWithPath("authorHandle").type(STRING).description("작성자 handle"),
                                fieldWithPath("authorName").type(STRING).description("작성자 이름"),
                                fieldWithPath("content").type(STRING).description("게시글 본문"),
                                fieldWithPath("imageUrls").type(ARRAY).description("이미지 URL 목록"),
                                subsectionWithPath("mediaItems").type(ARRAY).description("미디어 목록(이미지/동영상). 각 항목은 mediaType, url, thumbnailUrl, contentType을 포함"),
                                fieldWithPath("visibility").type(STRING).description("공개 범위"),
                                fieldWithPath("createdAt").type(STRING).description("생성 시각(ISO-8601)"),
                                fieldWithPath("likeCount").description("좋아요 수"),
                                fieldWithPath("commentCount").description("댓글 수"),
                                fieldWithPath("likedByMe").description("내가 좋아요 눌렀는지 여부"),
                                fieldWithPath("latitude").optional().type(VARIES).description("게시글 위치 위도. 위치가 없으면 null"),
                                fieldWithPath("longitude").optional().type(VARIES).description("게시글 위치 경도. 위치가 없으면 null"),
                                fieldWithPath("placeName").optional().type(VARIES).description("장소명. 위치/장소 정보가 없으면 null"),
                                fieldWithPath("distanceKm").optional().type(VARIES).description("nearby 조회 기준 거리(km). 일반 게시글 응답에서는 null")
                        )
                ));
    }

    @Test
    void 게시글_수정_성공_200(RestDocumentationContextProvider restDocumentation) throws Exception {
        MockMvc mockMvc = mockMvc(restDocumentation);

        var user = docs.saveUser("post-patch", "작성자");
        String token = docs.issueTokenFor(user);

        var createReq = new HashMap<String, Object>();
        createReq.put("content", "수정 전 본문");
        createReq.put("visibility", "ALL");

        String postId = objectMapper.readTree(
                mockMvc.perform(post("/api/posts")
                                .header(DocsTestSupport.headerName(), DocsTestSupport.auth(token))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(createReq)))
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString()
        ).get("postId").asText();

        var patchReq = new HashMap<String, Object>();
        patchReq.put("content", "수정 후 본문");
        patchReq.put("visibility", "FOLLOWERS");

        mockMvc.perform(patch("/api/posts/{postId}", postId)
                        .header(DocsTestSupport.headerName(), DocsTestSupport.auth(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(patchReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("수정 후 본문"))
                .andExpect(jsonPath("$.visibility").value("FOLLOWERS"))
                .andDo(document("posts-update",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(
                                headerWithName(DocsTestSupport.headerName()).description("Bearer {accessToken}")
                        ),
                        pathParameters(
                                parameterWithName("postId").description("수정할 게시글 ID(UUID)")
                        ),
                        requestFields(
                                fieldWithPath("content").type(STRING).optional().description("수정할 게시글 본문"),
                                fieldWithPath("visibility").type(STRING).optional().description("수정할 공개 범위(ALL | FOLLOWERS | PRIVATE)")
                        ),
                        responseFields(
                                fieldWithPath("postId").type(STRING).description("게시글 ID(UUID)"),
                                fieldWithPath("authorId").type(STRING).description("작성자 ID(UUID)"),
                                fieldWithPath("authorHandle").type(STRING).description("작성자 handle"),
                                fieldWithPath("authorName").type(STRING).description("작성자 이름"),
                                fieldWithPath("content").type(STRING).description("게시글 본문"),
                                fieldWithPath("imageUrls").type(ARRAY).description("이미지 URL 목록"),
                                subsectionWithPath("mediaItems").type(ARRAY).description("미디어 목록(이미지/동영상). 각 항목은 mediaType, url, thumbnailUrl, contentType을 포함"),
                                fieldWithPath("visibility").type(STRING).description("공개 범위"),
                                fieldWithPath("createdAt").type(STRING).description("생성 시각(ISO-8601)"),
                                fieldWithPath("likeCount").description("좋아요 수"),
                                fieldWithPath("commentCount").description("댓글 수"),
                                fieldWithPath("likedByMe").description("내가 좋아요 눌렀는지 여부"),
                                fieldWithPath("latitude").optional().type(VARIES).description("게시글 위치 위도. 위치가 없으면 null"),
                                fieldWithPath("longitude").optional().type(VARIES).description("게시글 위치 경도. 위치가 없으면 null"),
                                fieldWithPath("placeName").optional().type(VARIES).description("장소명. 위치/장소 정보가 없으면 null"),
                                fieldWithPath("distanceKm").optional().type(VARIES).description("nearby 조회 기준 거리(km). 일반 게시글 응답에서는 null")
                        )
                ));
    }

    @Test
    void 게시글_삭제_성공_204(RestDocumentationContextProvider restDocumentation) throws Exception {
        MockMvc mockMvc = mockMvc(restDocumentation);

        var user = docs.saveUser("del", "작성자");
        String token = docs.issueTokenFor(user);

        var createReq = new HashMap<String, Object>();
        createReq.put("content", "삭제 테스트");
        createReq.put("imageUrls", java.util.List.of("https://example.com/image1.jpg"));
        createReq.put("visibility", "FOLLOWERS");

        String postId = objectMapper.readTree(
                mockMvc.perform(post("/api/posts")
                                .header(DocsTestSupport.headerName(), DocsTestSupport.auth(token))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(createReq)))
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString()
        ).get("postId").asText();

        mockMvc.perform(delete("/api/posts/{postId}", postId)
                .header(DocsTestSupport.headerName(), DocsTestSupport.auth(token)))
                .andExpect(status().isNoContent())
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andDo(document("posts-delete",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(
                                headerWithName(DocsTestSupport.headerName()).description("Bearer {accessToken}")
                        ),
                        pathParameters(
                                parameterWithName("postId").description("삭제할 게시글 ID(UUID)")
                        )
                ));
    }
}
