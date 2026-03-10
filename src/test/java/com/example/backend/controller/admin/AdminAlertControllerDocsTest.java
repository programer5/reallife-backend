package com.example.backend.controller.admin;

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
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.JsonFieldType.BOOLEAN;
import static org.springframework.restdocs.payload.JsonFieldType.STRING;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.relaxedResponseFields;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@ExtendWith({RestDocumentationExtension.class, SpringExtension.class})
@Transactional
class AdminAlertControllerDocsTest {

    @Autowired
    WebApplicationContext context;

    @Autowired
    DocsTestSupport docs;

    private MockMvc mockMvc(RestDocumentationContextProvider restDocumentation) {
        return MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .apply(documentationConfiguration(restDocumentation))
                .build();
    }

    @Test
    void admin_alert_test_문서화(RestDocumentationContextProvider restDocumentation) throws Exception {
        var admin = docs.saveUser("alertdoc", "운영자");
        String token = docs.issueTokenFor(admin);

        mockMvc(restDocumentation)
                .perform(post("/admin/alerts/test")
                        .header(HttpHeaders.AUTHORIZATION, DocsTestSupport.auth(token)))
                .andExpect(status().isOk())
                .andDo(document("admin-alert-test-post",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(
                                headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer {accessToken}")
                        ),
                        relaxedResponseFields(
                                fieldWithPath("enabled").type(BOOLEAN).description("운영 알림 기능 활성화 여부"),
                                fieldWithPath("webhookConfigured").type(BOOLEAN).description("Slack webhook URL 설정 여부"),
                                fieldWithPath("sent").type(BOOLEAN).description("실제 Slack 테스트 메시지 전송 성공 여부"),
                                fieldWithPath("channel").type(STRING).description("알림 채널 종류"),
                                fieldWithPath("requestedBy").type(STRING).description("테스트 요청자 식별값"),
                                fieldWithPath("application").type(STRING).description("애플리케이션 이름/버전"),
                                fieldWithPath("message").type(STRING).description("테스트 결과 메시지"),
                                fieldWithPath("checkedAt").type(STRING).description("테스트 실행 시각")
                        )
                ));
    }
}