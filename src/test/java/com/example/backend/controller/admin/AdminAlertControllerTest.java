package com.example.backend.controller.admin;

import com.example.backend.controller.DocsTestSupport;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
@Transactional
class AdminAlertControllerTest {

    @Autowired
    WebApplicationContext context;

    @Autowired
    DocsTestSupport docs;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void admin_alert_test_인증없음_401() throws Exception {
        mockMvc.perform(post("/admin/alerts/test"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void admin_alert_test_응답구조_200() throws Exception {
        var admin = docs.saveUser("alertadmin", "운영자");
        String token = docs.issueTokenFor(admin);

        mockMvc.perform(post("/admin/alerts/test")
                        .header(DocsTestSupport.headerName(), DocsTestSupport.auth(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").exists())
                .andExpect(jsonPath("$.webhookConfigured").exists())
                .andExpect(jsonPath("$.sent").exists())
                .andExpect(jsonPath("$.channel").value("SLACK"))
                .andExpect(jsonPath("$.requestedBy").exists())
                .andExpect(jsonPath("$.application").exists())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.checkedAt").exists());
    }
}