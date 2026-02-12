package com.example.backend.controller.message;

import com.example.backend.controller.DocsTestSupport;
import com.example.backend.controller.message.dto.MessageSendRequest;
import com.example.backend.service.message.ConversationService;
import com.example.backend.service.message.MessageCommandService;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.util.List;
import java.util.UUID;

import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.JsonFieldType.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@ExtendWith({RestDocumentationExtension.class, SpringExtension.class})
@Transactional
class MessageQueryControllerDocsTest {

    @Autowired WebApplicationContext context;
    @Autowired DocsTestSupport docs;
    @Autowired ConversationService conversationService;
    @Autowired MessageCommandService messageCommandService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private MockMvc mockMvc(RestDocumentationContextProvider restDocumentation) {
        return MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .apply(documentationConfiguration(restDocumentation))
                .build();
    }

    @Test
    void 메시지목록_첫페이지_200(RestDocumentationContextProvider restDocumentation) throws Exception {
        var me = docs.saveUser("msgme", "나");
        var target = docs.saveUser("msgtarget", "상대");
        String token = docs.issueTokenFor(me);

        UUID conversationId = conversationService.createOrGetDirect(me.getId(), target.getId());

        // 메시지 3개 생성 (size=2면 hasNext=true)
        messageCommandService.send(me.getId(), conversationId, new MessageSendRequest("메시지 1", List.of()));
        messageCommandService.send(me.getId(), conversationId, new MessageSendRequest("메시지 2", List.of()));
        messageCommandService.send(me.getId(), conversationId, new MessageSendRequest("메시지 3", List.of()));

        mockMvc(restDocumentation)
                .perform(get("/api/conversations/{conversationId}/messages", conversationId)
                        .param("size", "2")
                        .header(HttpHeaders.AUTHORIZATION, DocsTestSupport.auth(token)))
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.hasNext").value(true))
                .andDo(document("messages-get",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(
                                headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer {accessToken}")
                        ),
                        pathParameters(
                                org.springframework.restdocs.request.RequestDocumentation.parameterWithName("conversationId")
                                        .description("대화방 ID(UUID)")
                        ),
                        queryParameters(
                                parameterWithName("cursor").optional().description("페이지 커서(createdAt|id). 없으면 첫 페이지"),
                                parameterWithName("size").optional().description("페이지 크기(기본 20, 최대 50)")
                        ),
                        responseFields(
                                fieldWithPath("items").type(ARRAY).description("메시지 목록(최신순)"),
                                fieldWithPath("items[].messageId").type(STRING).description("메시지 ID"),
                                fieldWithPath("items[].senderId").type(STRING).description("보낸 사람 ID"),
                                fieldWithPath("items[].content").optional().type(STRING).description("메시지 내용(없을 수 있음)"),
                                fieldWithPath("items[].createdAt").type(STRING).description("생성 시각"),

                                fieldWithPath("items[].attachments").type(ARRAY).description("첨부 목록"),

                                // ✅ 여기부터 optional
                                fieldWithPath("items[].attachments[].fileId").optional().type(STRING).description("파일 ID"),
                                fieldWithPath("items[].attachments[].url").optional().type(STRING).description("다운로드 URL"),
                                fieldWithPath("items[].attachments[].originalFilename").optional().type(STRING).description("원본 파일명"),
                                fieldWithPath("items[].attachments[].contentType").optional().type(STRING).description("MIME 타입"),
                                fieldWithPath("items[].attachments[].size").optional().type(NUMBER).description("파일 크기(bytes)"),

                                // ✅ optional() 중복 제거 (한 번만)
                                fieldWithPath("nextCursor").optional().type(STRING).description("다음 페이지 커서(없으면 null)"),
                                fieldWithPath("hasNext").type(BOOLEAN).description("다음 페이지 존재 여부")
                        )
                ));
    }

    @Test
    void 메시지목록_다음페이지_200(RestDocumentationContextProvider restDocumentation) throws Exception {
        var me = docs.saveUser("msgme", "나");
        var target = docs.saveUser("msgtarget", "상대");
        String token = docs.issueTokenFor(me);

        UUID conversationId = conversationService.createOrGetDirect(me.getId(), target.getId());

        messageCommandService.send(me.getId(), conversationId, new MessageSendRequest("메시지 1", List.of()));
        messageCommandService.send(me.getId(), conversationId, new MessageSendRequest("메시지 2", List.of()));
        messageCommandService.send(me.getId(), conversationId, new MessageSendRequest("메시지 3", List.of()));

        String first = mockMvc(restDocumentation)
                .perform(get("/api/conversations/{conversationId}/messages", conversationId)
                        .param("size", "2")
                        .header(HttpHeaders.AUTHORIZATION, DocsTestSupport.auth(token)))
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String nextCursor = objectMapper.readTree(first).get("nextCursor").asText();

        mockMvc(restDocumentation)
                .perform(get("/api/conversations/{conversationId}/messages", conversationId)
                        .param("size", "2")
                        .param("cursor", nextCursor)
                        .header(HttpHeaders.AUTHORIZATION, DocsTestSupport.auth(token)))
                .andExpect(status().isOk())
                .andDo(document("messages-get-next-page",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(
                                headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer {accessToken}")
                        ),
                        pathParameters(
                                org.springframework.restdocs.request.RequestDocumentation.parameterWithName("conversationId")
                                        .description("대화방 ID(UUID)")
                        ),
                        queryParameters(
                                parameterWithName("cursor").optional().description("페이지 커서(createdAt|id)"),
                                parameterWithName("size").optional().description("페이지 크기(기본 20, 최대 50)")
                        ),
                        responseFields(
                                fieldWithPath("items").type(ARRAY).description("메시지 목록(최신순)"),
                                fieldWithPath("items[].messageId").type(STRING).description("메시지 ID"),
                                fieldWithPath("items[].senderId").type(STRING).description("보낸 사람 ID"),
                                fieldWithPath("items[].content").optional().type(STRING).description("메시지 내용(없을 수 있음)"),
                                fieldWithPath("items[].createdAt").type(STRING).description("생성 시각"),

                                fieldWithPath("items[].attachments").type(ARRAY).description("첨부 목록"),

                                // ✅ 여기부터 optional
                                fieldWithPath("items[].attachments[].fileId").optional().type(STRING).description("파일 ID"),
                                fieldWithPath("items[].attachments[].url").optional().type(STRING).description("다운로드 URL"),
                                fieldWithPath("items[].attachments[].originalFilename").optional().type(STRING).description("원본 파일명"),
                                fieldWithPath("items[].attachments[].contentType").optional().type(STRING).description("MIME 타입"),
                                fieldWithPath("items[].attachments[].size").optional().type(NUMBER).description("파일 크기(bytes)"),

                                // ✅ optional() 중복 제거 (한 번만)
                                fieldWithPath("nextCursor").optional().type(STRING).description("다음 페이지 커서(없으면 null)"),
                                fieldWithPath("hasNext").type(BOOLEAN).description("다음 페이지 존재 여부")
                        )
                ));
    }
}