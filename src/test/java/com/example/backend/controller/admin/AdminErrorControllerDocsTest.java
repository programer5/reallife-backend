package com.example.backend.controller.admin;

import com.example.backend.controller.DocsTestSupport;
import com.example.backend.service.error.ErrorLogService;
import org.junit.jupiter.api.BeforeEach;
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
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.JsonFieldType.ARRAY;
import static org.springframework.restdocs.payload.JsonFieldType.STRING;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.relaxedResponseFields;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@ExtendWith({RestDocumentationExtension.class, SpringExtension.class})
@Transactional
class AdminErrorControllerDocsTest {

    @Autowired
    WebApplicationContext context;

    @Autowired
    DocsTestSupport docs;

    @Autowired
    ErrorLogService errorLogService;

    private MockMvc mockMvc(RestDocumentationContextProvider restDocumentation) {
        return MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .apply(documentationConfiguration(restDocumentation))
                .build();
    }

    @BeforeEach
    void setUp() {
        errorLogService.record(
                "IllegalStateException",
                "운영 문서화용 에러 로그 1",
                "/api/test/errors/1"
        );
        errorLogService.record(
                "RuntimeException",
                "운영 문서화용 에러 로그 2",
                "/api/test/errors/2"
        );
    }

    @Test
    void admin_errors_문서화(RestDocumentationContextProvider restDocumentation) throws Exception {
        var admin = docs.saveUser("errordoc", "운영자");
        String token = docs.issueTokenFor(admin);

        mockMvc(restDocumentation)
                .perform(get("/admin/errors")
                        .header(HttpHeaders.AUTHORIZATION, DocsTestSupport.auth(token)))
                .andExpect(status().isOk())
                .andDo(document("admin-errors-get",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(
                                headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer {accessToken}")
                        ),
                        relaxedResponseFields(
                                fieldWithPath("[]").type(ARRAY).description("최근 서버 에러 로그 목록"),
                                fieldWithPath("[].id").type(STRING).description("에러 로그 ID"),
                                fieldWithPath("[].type").type(STRING).description("예외 타입"),
                                fieldWithPath("[].message").type(STRING).description("에러 메시지"),
                                fieldWithPath("[].path").type(STRING).description("에러 발생 경로"),
                                fieldWithPath("[].createdAt").type(STRING).description("에러 발생 시각")
                        )
                ));
    }
}