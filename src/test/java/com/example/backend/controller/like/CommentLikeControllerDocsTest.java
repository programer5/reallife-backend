package com.example.backend.controller.like;

import com.example.backend.controller.DocsTestSupport;
import com.example.backend.domain.comment.Comment;
import com.example.backend.domain.user.User;
import com.example.backend.repository.comment.CommentRepository;
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
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@ExtendWith({RestDocumentationExtension.class, SpringExtension.class})
@Transactional
class CommentLikeControllerDocsTest {

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
    void 댓글_좋아요_204(RestDocumentationContextProvider restDocumentation) throws Exception {
        User me = docs.saveUser("cliker", "좋아요러");
        String token = docs.issueTokenFor(me);

        UUID postId = docs.savePost(me.getId(), "post for comment like").getId();
        UUID commentId = commentRepository.saveAndFlush(Comment.create(postId, me.getId(), "hi")).getId();

        mockMvc(restDocumentation)
                .perform(post("/api/comments/{commentId}/likes", commentId)
                        .header(HttpHeaders.AUTHORIZATION, DocsTestSupport.auth(token)))
                .andDo(print())
                .andExpect(status().isNoContent())
                .andDo(document("comment-likes-post-204",
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
    void 댓글_좋아요_취소_204(RestDocumentationContextProvider restDocumentation) throws Exception {
        User me = docs.saveUser("cunliker", "좋아요취소");
        String token = docs.issueTokenFor(me);

        UUID postId = docs.savePost(me.getId(), "post for comment unlike").getId();
        UUID commentId = commentRepository.saveAndFlush(Comment.create(postId, me.getId(), "hi")).getId();

        // like first to make unlike meaningful
        mockMvc(restDocumentation)
                .perform(post("/api/comments/{commentId}/likes", commentId)
                        .header(HttpHeaders.AUTHORIZATION, DocsTestSupport.auth(token)))
                .andExpect(status().isNoContent());

        mockMvc(restDocumentation)
                .perform(delete("/api/comments/{commentId}/likes", commentId)
                        .header(HttpHeaders.AUTHORIZATION, DocsTestSupport.auth(token)))
                .andDo(print())
                .andExpect(status().isNoContent())
                .andDo(document("comment-likes-delete-204",
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
}
