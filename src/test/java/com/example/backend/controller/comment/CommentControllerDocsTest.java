package com.example.backend.controller.comment;

import com.example.backend.controller.DocsTestSupport;
import com.example.backend.domain.comment.Comment;
import com.example.backend.domain.user.User;
import com.example.backend.repository.comment.CommentRepository;
import com.example.backend.restdocs.ErrorResponseSnippet;
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

import java.util.UUID;

import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@ExtendWith({RestDocumentationExtension.class, SpringExtension.class})
@Transactional
class CommentControllerDocsTest {

    @Autowired WebApplicationContext context;
    @Autowired DocsTestSupport docs;
    @Autowired CommentRepository commentRepository;

    private MockMvc mockMvc(RestDocumentationContextProvider restDocumentation) {
        return MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .apply(documentationConfiguration(restDocumentation))
                .build();
    }

    @Test
    void 댓글_생성_201(RestDocumentationContextProvider restDocumentation) throws Exception {
        User me = docs.saveUser("commenter", "댓글러");
        String token = docs.issueTokenFor(me);

        UUID postId = docs.savePost(me.getId(), "post for comment").getId();

        mockMvc(restDocumentation)
                .perform(post("/api/posts/{postId}/comments", postId)
                        .contentType("application/json")
                        .content("""
                                {
                                  "content": "nice!"
                                }
                                """)
                        .header(HttpHeaders.AUTHORIZATION, DocsTestSupport.auth(token)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andDo(document("comments-create-201",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(
                                headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer {accessToken}")
                        ),
                        pathParameters(
                                parameterWithName("postId").description("게시글 ID")
                        ),
                        requestFields(
                                fieldWithPath("content").description("댓글 내용")
                        ),
                        responseFields(
                                fieldWithPath("commentId").description("댓글 ID"),
                                fieldWithPath("postId").description("게시글 ID"),
                                fieldWithPath("userId").description("작성자 유저 ID(UUID)"),
                                fieldWithPath("content").description("댓글 내용"),
                                fieldWithPath("createdAt").description("작성 시간(ISO-8601)")
                        )
                ));
    }

    @Test
    void 댓글목록_조회_200(RestDocumentationContextProvider restDocumentation) throws Exception {
        User me = docs.saveUser("reader", "리더");
        String token = docs.issueTokenFor(me);

        UUID postId = docs.savePost(me.getId(), "post for comment").getId();

        // ✅ 문서/스니펫 생성을 위해 최소 1건의 댓글을 미리 생성
        commentRepository.saveAndFlush(Comment.create(postId, me.getId(), "first comment"));

        mockMvc(restDocumentation)
                .perform(get("/api/posts/{postId}/comments", postId)
                        .param("size", "10")
                        .header(HttpHeaders.AUTHORIZATION, DocsTestSupport.auth(token)))
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(document("comments-list-200",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(
                                headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer {accessToken}")
                        ),
                        pathParameters(
                                parameterWithName("postId").description("게시글 ID")
                        ),
                        queryParameters(
                                parameterWithName("cursor")
                                        .optional()
                                        .description("페이지 커서(포맷: createdAt|commentId, 예: 2026-02-10T11:22:33|4b2d... ). 없으면 첫 페이지"),
                                parameterWithName("size").optional().description("페이지 크기(기본 20, 최대 50)")
                        ),
                        responseFields(
                                fieldWithPath("items").description("댓글 목록"),
                                fieldWithPath("items[].commentId").description("댓글 ID"),
                                fieldWithPath("items[].userId").description("작성자 유저 ID(UUID)"),
                                fieldWithPath("items[].handle").description("작성자 핸들"),
                                fieldWithPath("items[].name").description("작성자 이름"),
                                fieldWithPath("items[].content").description("댓글 내용"),
                                fieldWithPath("items[].createdAt").description("작성 시간(ISO-8601)"),
                                fieldWithPath("nextCursor").optional().description("다음 페이지 커서"),
                                fieldWithPath("hasNext").description("다음 페이지 존재 여부")
                        )
                ));
    }

    @Test
    void 댓글_삭제_204(RestDocumentationContextProvider restDocumentation) throws Exception {
        User me = docs.saveUser("deleter", "삭제러");
        String token = docs.issueTokenFor(me);

        // ✅ 실제 저장된 UUID commentId 사용
        UUID postId = docs.savePost(me.getId(), "post for delete").getId();
        UUID commentId = commentRepository.saveAndFlush(Comment.create(postId, me.getId(), "to delete")).getId();

        mockMvc(restDocumentation)
                .perform(delete("/api/comments/{commentId}", commentId)
                        .header(HttpHeaders.AUTHORIZATION, DocsTestSupport.auth(token)))
                .andDo(print())
                .andExpect(status().isNoContent())
                .andDo(document("comments-delete-204",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(
                                headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer {accessToken}")
                        ),
                        pathParameters(
                                parameterWithName("commentId").description("댓글 ID")
                        )
                ));
    }

    @Test
    void 댓글_생성_content_blank_400(RestDocumentationContextProvider restDocumentation) throws Exception {
        User me = docs.saveUser("commenter2", "댓글러2");
        String token = docs.issueTokenFor(me);

        UUID postId = docs.savePost(me.getId(), "post for comment").getId();

        mockMvc(restDocumentation)
                .perform(post("/api/posts/{postId}/comments", postId)
                        .contentType("application/json")
                        .content("""
                                {
                                  "content": "   "
                                }
                                """)
                        .header(HttpHeaders.AUTHORIZATION, DocsTestSupport.auth(token)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andDo(document("comments-create-400-content-blank",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(
                                headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer {accessToken}")
                        ),
                        pathParameters(
                                parameterWithName("postId").description("게시글 ID")
                        ),
                        requestFields(
                                fieldWithPath("content").description("댓글 내용(blank 불가)")
                        ),
                        // ✅ fieldErrors[].field / fieldErrors[].reason 까지 문서화
                        responseFields(ErrorResponseSnippet.validation())
                ));
    }
}