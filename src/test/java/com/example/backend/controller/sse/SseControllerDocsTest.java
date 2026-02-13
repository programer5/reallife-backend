package com.example.backend.controller.sse;

import com.example.backend.controller.DocsTestSupport;
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
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.restdocs.headers.HeaderDocumentation.*;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@ExtendWith({RestDocumentationExtension.class, SpringExtension.class})
@Transactional
class SseControllerDocsTest {

    @Autowired private WebApplicationContext context;
    @Autowired private DocsTestSupport docs;

    private MockMvc mockMvc(RestDocumentationContextProvider restDocumentation) {
        return MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .apply(documentationConfiguration(restDocumentation))
                .build();
    }

    @Test
    void SSE_구독_요청_문서화_비동기_시작_확인(RestDocumentationContextProvider restDocumentation) throws Exception {
        MockMvc mockMvc = mockMvc(restDocumentation);

        var me = docs.saveUser("sse", "나");
        String token = docs.issueTokenFor(me);

        // ✅ SSE는 응답이 끝나지 않으니 asyncDispatch로 완료를 기다리면 테스트가 멈출 수 있음
        mockMvc.perform(get("/api/sse/subscribe")
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .header(DocsTestSupport.headerName(), DocsTestSupport.auth(token))
                        .header("Last-Event-ID", "optional-last-event-id"))
                .andExpect(status().isOk())
                .andExpect(request().asyncStarted())
                .andDo(document("sse-subscribe",
                        preprocessRequest(prettyPrint()),
                        requestHeaders(
                                headerWithName(DocsTestSupport.headerName()).description("Bearer {accessToken}"),
                                headerWithName("Last-Event-ID").optional()
                                        .description("재연결 시 마지막으로 수신한 SSE 이벤트 ID. 서버는 이후 이벤트를 replay할 수 있음.")
                        )
                ));
    }
}