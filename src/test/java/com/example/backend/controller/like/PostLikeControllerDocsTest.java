package com.example.backend.controller.like;

import com.example.backend.controller.DocsTestSupport;
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
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@ExtendWith({RestDocumentationExtension.class, SpringExtension.class})
@Transactional
class PostLikeControllerDocsTest {

    @Autowired WebApplicationContext context;
    @Autowired DocsTestSupport docs;

    private MockMvc mockMvc(RestDocumentationContextProvider restDocumentation) {
        return MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .apply(documentationConfiguration(restDocumentation))
                .build();
    }

    @Test
    void 좋아요_성공_204(RestDocumentationContextProvider restDocumentation) throws Exception {
        var me = docs.saveUser("like", "좋아요유저");
        String token = docs.issueTokenFor(me);

        var author = docs.saveUser("author", "작성자");
        var post = docs.savePost(author.getId(), "좋아요 테스트");

        mockMvc(restDocumentation)
                .perform(post("/api/posts/{postId}/likes", post.getId())
                        .header(HttpHeaders.AUTHORIZATION, DocsTestSupport.auth(token)))
                .andExpect(status().isNoContent())
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andDo(document("post-likes-create-204",
                        requestHeaders(
                                headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer {accessToken}")
                        ),
                        pathParameters(
                                parameterWithName("postId").description("게시글 ID(UUID)")
                        )
                ));
    }

    @Test
    void 좋아요_취소_성공_204(RestDocumentationContextProvider restDocumentation) throws Exception {
        var me = docs.saveUser("unlike", "취소유저");
        String token = docs.issueTokenFor(me);

        var author = docs.saveUser("author2", "작성자");
        var post = docs.savePost(author.getId(), "좋아요 취소 테스트");

        // 먼저 좋아요
        mockMvc(restDocumentation)
                .perform(post("/api/posts/{postId}/likes", post.getId())
                        .header(HttpHeaders.AUTHORIZATION, DocsTestSupport.auth(token)))
                .andExpect(status().isNoContent());

        // 취소
        mockMvc(restDocumentation)
                .perform(delete("/api/posts/{postId}/likes", post.getId())
                        .header(HttpHeaders.AUTHORIZATION, DocsTestSupport.auth(token)))
                .andExpect(status().isNoContent())
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andDo(document("post-likes-delete-204",
                        requestHeaders(
                                headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer {accessToken}")
                        ),
                        pathParameters(
                                parameterWithName("postId").description("게시글 ID(UUID)")
                        )
                ));
    }
}
