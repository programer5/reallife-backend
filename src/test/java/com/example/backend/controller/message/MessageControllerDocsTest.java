package com.example.backend.controller.message;

import com.example.backend.controller.DocsTestSupport;
import com.example.backend.controller.message.dto.MessageSendRequest;
import com.example.backend.service.message.ConversationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.payload.JsonFieldType.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@ExtendWith({RestDocumentationExtension.class, SpringExtension.class})
@Transactional
class MessageControllerDocsTest {

    @Autowired WebApplicationContext context;
    @Autowired DocsTestSupport docs;
    @Autowired ConversationService conversationService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private MockMvc mockMvc(RestDocumentationContextProvider restDocumentation) {
        return MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .apply(documentationConfiguration(restDocumentation))
                .build();
    }

    @Test
    void 메시지전송_200(RestDocumentationContextProvider restDocumentation) throws Exception {
        MockMvc mockMvc = mockMvc(restDocumentation);

        var me = docs.saveUser("sendme", "나");
        var target = docs.saveUser("sendtarget", "상대");
        String token = docs.issueTokenFor(me);

        UUID conversationId = conversationService.createOrGetDirect(me.getId(), target.getId());

        // controller DTO 그대로 사용
        MessageSendRequest req = new MessageSendRequest("안녕! (REST Docs)", List.of());

        mockMvc.perform(post("/api/conversations/{conversationId}/messages", conversationId)
                        .header(HttpHeaders.AUTHORIZATION, DocsTestSupport.auth(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messageId").isNotEmpty())
                .andExpect(jsonPath("$.conversationId").value(conversationId.toString()))
                .andExpect(jsonPath("$.senderId").isNotEmpty())
                .andExpect(jsonPath("$.content").value("안녕! (REST Docs)"))
                .andExpect(jsonPath("$.attachments").isArray())
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andDo(document("messages-send",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(
                                headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer {accessToken}")
                        ),
                        pathParameters(
                                parameterWithName("conversationId").description("대화방 ID(UUID)")
                        ),
                        requestFields(
                                fieldWithPath("content").optional().type(STRING).description("메시지 내용(최대 5000자)"),
                                fieldWithPath("attachmentIds").type(ARRAY).description("첨부 파일 ID 목록(없으면 빈 배열)")
                        ),
                        responseFields(
                                fieldWithPath("messageId").type(STRING).description("메시지 ID"),
                                fieldWithPath("conversationId").type(STRING).description("대화방 ID"),
                                fieldWithPath("senderId").type(STRING).description("발신자 ID"),
                                fieldWithPath("content").optional().type(STRING).description("메시지 내용"),
                                fieldWithPath("attachments").type(ARRAY).description("첨부 파일 정보 목록"),
                                fieldWithPath("attachments[].fileId").optional().type(STRING).description("파일 ID"),
                                fieldWithPath("attachments[].url").optional().type(STRING).description("다운로드 URL"),
                                fieldWithPath("attachments[].originalFilename").optional().type(STRING).description("원본 파일명"),
                                fieldWithPath("attachments[].contentType").optional().type(STRING).description("MIME 타입"),
                                fieldWithPath("attachments[].size").optional().type(NUMBER).description("파일 크기(bytes)"),
                                fieldWithPath("createdAt").type(STRING).description("생성 시각")
                        )
                ));
    }
}