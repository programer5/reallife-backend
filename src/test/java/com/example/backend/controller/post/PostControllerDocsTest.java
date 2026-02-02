package com.example.backend.controller.post;

import com.example.backend.controller.DocsTestSupport;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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

import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.payload.JsonFieldType.ARRAY;
import static org.springframework.restdocs.payload.JsonFieldType.STRING;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.snippet.Attributes.key;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@ExtendWith({RestDocumentationExtension.class, SpringExtension.class})
@Transactional
class PostControllerDocsTest {

    @Autowired private WebApplicationContext context;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private DocsTestSupport docs;

    private MockMvc mockMvc(RestDocumentationContextProvider restDocumentation) {
        return MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity()) // ✅ 추가
                .apply(documentationConfiguration(restDocumentation))
                .build();
    }

    @Test
    void 게시글_생성_성공_201(RestDocumentationContextProvider restDocumentation) throws Exception {
        MockMvc mockMvc = mockMvc(restDocumentation);

        var user = docs.saveUser("post", "작성자");
        String token = docs.issueTokenFor(user);

        var req = new HashMap<String, Object>();
        req.put("content", "팔로워에게만 공유합니다 🙂");
        req.put("imageUrls", java.util.List.of("https://example.com/image1.jpg"));
        req.put("visibility", "FOLLOWERS"); // ✅ 실제 응답과 동일하게

        mockMvc.perform(post("/api/posts")
                        .header(DocsTestSupport.headerName(), DocsTestSupport.auth(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.postId").exists())
                .andExpect(jsonPath("$.authorId").exists())
                .andExpect(jsonPath("$.visibility").value("FOLLOWERS"))
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andDo(document("posts-create",
                        requestHeaders(
                                headerWithName(DocsTestSupport.headerName()).description("Bearer {accessToken}")
                        ),
                        requestFields(
                                fieldWithPath("content").type(STRING).description("게시글 본문"),
                                fieldWithPath("imageUrls").optional().type(ARRAY).description("이미지 URL 목록"),
                                fieldWithPath("visibility").optional().type(STRING)
                                        .attributes(key("constraints").value("PUBLIC | FOLLOWERS | PRIVATE"))
                                        .description("공개 범위")
                        ),
                        responseFields(
                                fieldWithPath("postId").type(STRING).description("생성된 게시글 ID(UUID)"),
                                fieldWithPath("authorId").type(STRING).description("작성자 ID(UUID)"),
                                fieldWithPath("content").type(STRING).description("게시글 본문"),
                                fieldWithPath("imageUrls").type(ARRAY).description("이미지 URL 목록"),
                                fieldWithPath("visibility").type(STRING).description("공개 범위"),
                                fieldWithPath("createdAt").type(STRING).description("생성 시각(ISO-8601)")
                        )
                ));
    }

    @Test
    void 게시글_상세조회_성공_200(RestDocumentationContextProvider restDocumentation) throws Exception {
        MockMvc mockMvc = mockMvc(restDocumentation);

        var user = docs.saveUser("detail", "작성자");
        String token = docs.issueTokenFor(user);

        // 1) 게시글 생성해서 postId 확보
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

        // 2) 상세조회
        mockMvc.perform(get("/api/posts/{postId}", postId)
                .header(DocsTestSupport.headerName(), DocsTestSupport.auth(token)))
                .andExpect(status().isOk())
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andDo(document("posts-get",
                        requestHeaders(
                                headerWithName(DocsTestSupport.headerName()).description("Bearer {accessToken}")
                        ),
                        // 응답은 create와 동일한 스펙으로 문서화(필드 타입 고정)
                        responseFields(
                                fieldWithPath("postId").type(STRING).description("게시글 ID(UUID)"),
                                fieldWithPath("authorId").type(STRING).description("작성자 ID(UUID)"),
                                fieldWithPath("content").type(STRING).description("게시글 본문"),
                                fieldWithPath("imageUrls").type(ARRAY).description("이미지 URL 목록"),
                                fieldWithPath("visibility").type(STRING).description("공개 범위"),
                                fieldWithPath("createdAt").type(STRING).description("생성 시각(ISO-8601)")
                        )
                ));
    }

    @Test
    void 게시글_삭제_성공_204(RestDocumentationContextProvider restDocumentation) throws Exception {
        MockMvc mockMvc = mockMvc(restDocumentation);

        var user = docs.saveUser("del", "작성자");
        String token = docs.issueTokenFor(user);

        // 게시글 생성 → postId 확보
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

        // 삭제
        mockMvc.perform(delete("/api/posts/{postId}", postId)
                .header(DocsTestSupport.headerName(), DocsTestSupport.auth(token)))
                .andExpect(status().isNoContent())
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andDo(document("posts-delete",
                        requestHeaders(
                                headerWithName(DocsTestSupport.headerName()).description("Bearer {accessToken}")
                        )
                ));
    }
}
