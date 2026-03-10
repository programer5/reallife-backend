package com.example.backend.controller.admin;

import com.example.backend.controller.DocsTestSupport;
import com.example.backend.domain.ops.OpsAlertLog;
import com.example.backend.domain.ops.OpsAlertLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
@Transactional
class AdminAlertHistoryControllerTest {

    @Autowired
    WebApplicationContext context;

    @Autowired
    DocsTestSupport docs;

    @Autowired
    OpsAlertLogRepository opsAlertLogRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void admin_alert_history_인증없음_401() throws Exception {
        mockMvc.perform(get("/admin/alerts/history"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void admin_alert_history_응답구조_200() throws Exception {
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

        var admin = docs.saveUser("alertHistoryAdmin", "운영자");
        String token = docs.issueTokenFor(admin);

        mockMvc.perform(get("/admin/alerts/history")
                        .header(DocsTestSupport.headerName(), DocsTestSupport.auth(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items[0].channel").value("SLACK"))
                .andExpect(jsonPath("$.items[0].alertKey").value("manual:test"))
                .andExpect(jsonPath("$.items[0].title").value("테스트 알림"))
                .andExpect(jsonPath("$.items[0].status").value("SENT"));
    }
}