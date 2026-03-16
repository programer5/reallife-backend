package com.example.backend.controller.home;

import com.example.backend.controller.DocsTestSupport;
import com.example.backend.domain.pin.ConversationPin;
import com.example.backend.repository.pin.ConversationPinRepository;
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

import java.time.LocalDateTime;
import java.util.UUID;

import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.JsonFieldType.ARRAY;
import static org.springframework.restdocs.payload.JsonFieldType.NUMBER;
import static org.springframework.restdocs.payload.JsonFieldType.STRING;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@ExtendWith({RestDocumentationExtension.class, SpringExtension.class})
@Transactional
class HomeTodayWidgetControllerDocsTest {

    @Autowired WebApplicationContext context;
    @Autowired DocsTestSupport docs;
    @Autowired ConversationPinRepository pinRepository;

    private MockMvc mockMvc(RestDocumentationContextProvider restDocumentation) {
        return MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .apply(documentationConfiguration(restDocumentation))
                .build();
    }

    @Test
    void 홈_Today_Widget_조회_200(RestDocumentationContextProvider restDocumentation) throws Exception {
        var me = docs.saveUser("home-today", "나");
        String token = docs.issueTokenFor(me);
        UUID conversationId = UUID.randomUUID();
        UUID sourceMessageId = UUID.randomUUID();

        pinRepository.saveAndFlush(ConversationPin.createSchedule(
                conversationId,
                me.getId(),
                sourceMessageId,
                "모란 먹켓치킨",
                "모란 먹켓치킨",
                LocalDateTime.now().withHour(23).withMinute(0).withSecond(0).withNano(0)
        ));

        mockMvc(restDocumentation)
                .perform(get("/api/home/today-widget")
                        .header(HttpHeaders.AUTHORIZATION, DocsTestSupport.auth(token)))
                .andExpect(status().isOk())
                .andDo(document("home-today-widget-get",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(
                                headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer {accessToken}")
                        ),
                        responseFields(
                                fieldWithPath("summary.total").type(NUMBER).description("오늘 일정 총 개수"),
                                fieldWithPath("summary.upcoming").type(NUMBER).description("아직 남은 일정 개수"),
                                fieldWithPath("summary.done").type(NUMBER).description("완료된 일정 개수"),
                                fieldWithPath("items").type(ARRAY).description("오늘 위젯에 노출할 액션 목록"),
                                fieldWithPath("items[].pinId").type(STRING).description("핀 ID(UUID)"),
                                fieldWithPath("items[].conversationId").type(STRING).description("원본 대화방 ID(UUID)"),
                                fieldWithPath("items[].sourceMessageId").type(STRING).optional().description("원본 메시지 ID(UUID)"),
                                fieldWithPath("items[].type").type(STRING).description("액션 타입"),
                                fieldWithPath("items[].title").type(STRING).description("액션 제목"),
                                fieldWithPath("items[].placeText").type(STRING).optional().description("장소 텍스트"),
                                fieldWithPath("items[].startAt").type(STRING).optional().description("시작 시각"),
                                fieldWithPath("items[].remindAt").type(STRING).optional().description("리마인드 시각"),
                                fieldWithPath("items[].status").type(STRING).description("액션 상태")
                        )
                ));
    }
}
