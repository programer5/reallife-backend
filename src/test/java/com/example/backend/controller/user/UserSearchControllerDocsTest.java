package com.example.backend.controller.user;

import com.example.backend.controller.DocsTestSupport;
import com.example.backend.domain.user.User;
import com.example.backend.repository.user.UserRepository;
import com.example.backend.restdocs.ErrorResponseSnippet;
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
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ExtendWith({RestDocumentationExtension.class, SpringExtension.class})
@Transactional
class UserSearchControllerDocsTest {

    @Autowired WebApplicationContext context;
    @Autowired DocsTestSupport docs;
    @Autowired UserRepository userRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private MockMvc mockMvc(RestDocumentationContextProvider restDocumentation) {
        return MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .apply(documentationConfiguration(restDocumentation))
                .build();
    }

    private User saveFixedUser(String email, String handle, String name) {
        // 네 프로젝트 컨벤션: new User(email, handle, password, name)
        return userRepository.saveAndFlush(new User(email, handle, "encoded", name));
    }

    @Test
    void 유저검색_첫페이지_200(RestDocumentationContextProvider restDocumentation) throws Exception {
        var me = docs.saveUser("searchme", "나");
        String token = docs.issueTokenFor(me);

        // q="kim" 기준 정렬 테스트용 데이터
        saveFixedUser("kim@test.com", "kim", "김철수");        // rank 0
        saveFixedUser("kimchi@test.com", "kimchi", "김치");    // rank 1
        saveFixedUser("xx@test.com", "xx", "kim영희");         // name contains -> rank 3
        saveFixedUser("akim@test.com", "akim", "아킴");        // handle contains -> rank 3

        mockMvc(restDocumentation)
                .perform(get("/api/users/search")
                        .param("q", "kim")
                        .param("size", "2")
                        .header(HttpHeaders.AUTHORIZATION, DocsTestSupport.auth(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.hasNext").value(true))
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andDo(document("users-search-200",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(
                                headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer {accessToken}")
                        ),
                        queryParameters(
                                parameterWithName("q").description("검색어(handle/name)"),
                                parameterWithName("cursor").optional().description("페이지 커서(rank|followerCount|handle|userId). 없으면 첫 페이지"),
                                parameterWithName("size").optional().description("페이지 크기(기본 20, 최대 50)")
                        ),
                        responseFields(
                                fieldWithPath("items").description("검색 결과 목록"),
                                fieldWithPath("items[].userId").description("유저 ID(UUID)"),
                                fieldWithPath("items[].handle").description("유저 핸들(@아이디)"),
                                fieldWithPath("items[].name").description("유저 이름"),
                                fieldWithPath("items[].followerCount").description("팔로워 수"),
                                fieldWithPath("items[].rank").description("정렬 랭크(0=prefix, 1=contains)"),
                                fieldWithPath("items[].isFollowing").description("내가 해당 유저를 팔로우 중인지 여부"),
                                fieldWithPath("nextCursor").optional().description("다음 페이지 커서(없으면 null)"),
                                fieldWithPath("hasNext").description("다음 페이지 존재 여부")
                        )
                ));
    }

    @Test
    void 유저검색_다음페이지_200(RestDocumentationContextProvider restDocumentation) throws Exception {
        var me = docs.saveUser("searchme", "나");
        String token = docs.issueTokenFor(me);

        saveFixedUser("kim@test.com", "kim", "김철수");
        saveFixedUser("kimchi@test.com", "kimchi", "김치");
        saveFixedUser("xx@test.com", "xx", "kim영희");
        saveFixedUser("akim@test.com", "akim", "아킴");

        String first = mockMvc(restDocumentation)
                .perform(get("/api/users/search")
                        .param("q", "kim")
                        .param("size", "2")
                        .header(HttpHeaders.AUTHORIZATION, DocsTestSupport.auth(token)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String nextCursor = objectMapper.readTree(first).get("nextCursor").asText();

        mockMvc(restDocumentation)
                .perform(get("/api/users/search")
                        .param("q", "kim")
                        .param("size", "2")
                        .param("cursor", nextCursor)
                        .header(HttpHeaders.AUTHORIZATION, DocsTestSupport.auth(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andDo(document("users-search-200-next-page",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(
                                headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer {accessToken}")
                        ),
                        queryParameters(
                                parameterWithName("q").description("검색어(handle/name)"),
                                parameterWithName("cursor").optional().description("페이지 커서(rank|followerCount|handle|userId). 없으면 첫 페이지"),
                                parameterWithName("size").optional().description("페이지 크기(기본 20, 최대 50)")
                        ),
                        responseFields(
                                fieldWithPath("items").description("검색 결과 목록"),
                                fieldWithPath("items[].userId").description("유저 ID(UUID)"),
                                fieldWithPath("items[].handle").description("유저 핸들(@아이디)"),
                                fieldWithPath("items[].name").description("유저 이름"),
                                fieldWithPath("items[].followerCount").description("팔로워 수"),
                                fieldWithPath("items[].rank").description("정렬 랭크(0=prefix, 1=contains)"),
                                fieldWithPath("items[].isFollowing").description("내가 해당 유저를 팔로우 중인지 여부"),
                                fieldWithPath("nextCursor").optional().description("다음 페이지 커서(없으면 null)"),
                                fieldWithPath("hasNext").description("다음 페이지 존재 여부")
                        )
                ));
    }

    @Test
    void 유저검색_q_blank_400(RestDocumentationContextProvider restDocumentation) throws Exception {
        var me = docs.saveUser("searchme", "나");
        String token = docs.issueTokenFor(me);

        mockMvc(restDocumentation)
                .perform(get("/api/users/search")
                        .param("q", "   ")
                        .header(HttpHeaders.AUTHORIZATION, DocsTestSupport.auth(token)))
                .andExpect(status().isBadRequest())
                .andDo(document("users-search-400-q-blank",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(
                                headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer {accessToken}")
                        ),
                        queryParameters(
                                parameterWithName("q").description("검색어(handle/name) (blank 불가)"),
                                parameterWithName("cursor").optional().description("페이지 커서(rank|followerCount|handle|userId). 없으면 첫 페이지"),
                                parameterWithName("size").optional().description("페이지 크기(기본 20, 최대 50)")
                        ),
                        responseFields(ErrorResponseSnippet.common())
                ));
    }

    @Test
    void 유저검색_size_범위초과_400(RestDocumentationContextProvider restDocumentation) throws Exception {
        var me = docs.saveUser("searchme", "나");
        String token = docs.issueTokenFor(me);

        mockMvc(restDocumentation)
                .perform(get("/api/users/search")
                        .param("q", "kim")
                        .param("size", "999")
                        .header(HttpHeaders.AUTHORIZATION, DocsTestSupport.auth(token)))
                .andExpect(status().isBadRequest())
                .andDo(document("users-search-400-size-range",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(
                                headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer {accessToken}")
                        ),
                        queryParameters(
                                parameterWithName("q").description("검색어(handle/name)"),
                                parameterWithName("cursor").optional().description("페이지 커서(rank|followerCount|handle|userId). 없으면 첫 페이지"),
                                parameterWithName("size").optional().description("페이지 크기(1~50)")
                        ),
                        responseFields(ErrorResponseSnippet.common())
                ));
    }
}