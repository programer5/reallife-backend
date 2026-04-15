package com.example.backend.controller.admin;

import com.example.backend.controller.DocsTestSupport;
import com.example.backend.domain.error.OpsAlertLog;
import com.example.backend.repository.error.OpsAlertLogRepository;
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
class AdminAlertHistoryControllerDocsTest {

    @Autowired
    WebApplicationContext context;

    @Autowired
    DocsTestSupport docs;

    @Autowired
    OpsAlertLogRepository opsAlertLogRepository;

    private MockMvc mockMvc(RestDocumentationContextProvider restDocumentation) {
        return MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .apply(documentationConfiguration(restDocumentation))
                .build();
    }

    @Test
    void admin_alert_history_문서화(RestDocumentationContextProvider restDocumentation) throws Exception {
        opsAlertLogRepository.save(
                OpsAlertLog.of(
                        "SLACK",
                        "manual:test",
                        "테스트 알림",
                        "테스트 본문",
                        "INFO",
                        "SENT",
                        "tester"
                )
        );

        var admin = docs.saveUserExact("alerthistorydoc@test.com", "alerthistorydoc", "운영자");
        String token = docs.issueTokenFor(admin);

        mockMvc(restDocumentation)
                .perform(get("/admin/alerts/history")
                        .header(HttpHeaders.AUTHORIZATION, DocsTestSupport.auth(token)))
                .andExpect(status().isOk())
                .andDo(document("admin-alert-history-get",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(
                                headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer {accessToken} (ops allowlist 포함 계정)")
                        ),
                        relaxedResponseFields(
                                fieldWithPath("items").type(ARRAY).description("최근 운영 알림 이력 목록"),
                                fieldWithPath("items[].id").type(STRING).description("운영 알림 로그 ID(UUID)"),
                                fieldWithPath("items[].channel").type(STRING).description("알림 채널 종류"),
                                fieldWithPath("items[].alertKey").type(STRING).description("알림 분류 키"),
                                fieldWithPath("items[].title").type(STRING).description("알림 제목"),
                                fieldWithPath("items[].body").type(STRING).description("알림 본문"),
                                fieldWithPath("items[].level").type(STRING).description("알림 심각도"),
                                fieldWithPath("items[].status").type(STRING).description("알림 전송 결과"),
                                fieldWithPath("items[].requestedBy").type(STRING).description("수동 요청자 식별값"),
                                fieldWithPath("items[].createdAt").type(STRING).description("생성 시각")
                        )
                ));
    }
}