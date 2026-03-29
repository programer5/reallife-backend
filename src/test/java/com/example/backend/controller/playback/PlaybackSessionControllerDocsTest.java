package com.example.backend.controller.playback;

import com.example.backend.controller.DocsTestSupport;
import com.example.backend.controller.playback.dto.PlaybackSessionCreateRequest;
import com.example.backend.controller.playback.dto.PlaybackSessionStateUpdateRequest;
import com.example.backend.domain.playback.PlaybackMediaKind;
import com.example.backend.domain.playback.PlaybackState;
import com.example.backend.service.message.ConversationService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.UUID;

import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.JsonFieldType.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@ExtendWith({RestDocumentationExtension.class, SpringExtension.class})
@Transactional
class PlaybackSessionControllerDocsTest {

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
    void 공동플레이_생성_조회_상태변경_종료(RestDocumentationContextProvider restDocumentation) throws Exception {
        MockMvc mockMvc = mockMvc(restDocumentation);

        var me = docs.saveUser("playme", "호스트");
        var target = docs.saveUser("playtarget", "게스트");
        String token = docs.issueTokenFor(me);
        UUID conversationId = conversationService.createOrGetDirect(me.getId(), target.getId());

        PlaybackSessionCreateRequest createRequest = new PlaybackSessionCreateRequest(
                PlaybackMediaKind.MUSIC,
                "비 오는 날 같이 듣기",
                "https://example.com/listen/playlist-1",
                "https://example.com/thumb.jpg"
        );

        String createResponse = mockMvc.perform(post("/api/conversations/{conversationId}/playback-sessions", conversationId)
                        .header(HttpHeaders.AUTHORIZATION, DocsTestSupport.auth(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").isNotEmpty())
                .andExpect(jsonPath("$.conversationId").value(conversationId.toString()))
                .andExpect(jsonPath("$.mediaKind").value("MUSIC"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andDo(document("playback-sessions-create",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(
                                headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer {accessToken}")
                        ),
                        pathParameters(
                                parameterWithName("conversationId").description("대화방 ID(UUID)")
                        ),
                        requestFields(
                                fieldWithPath("mediaKind").type(STRING).description("미디어 종류(MOVIE, MUSIC, VIDEO, LINK)"),
                                fieldWithPath("title").type(STRING).description("세션 제목"),
                                fieldWithPath("sourceUrl").type(STRING).description("같이 볼/들을 외부 링크 URL"),
                                fieldWithPath("thumbnailUrl").optional().type(STRING).description("썸네일 URL(옵션)")
                        ),
                        responseFields(sessionResponseFields())
                ))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode created = objectMapper.readTree(createResponse);
        String sessionId = created.get("sessionId").asText();

        mockMvc.perform(get("/api/conversations/{conversationId}/playback-sessions", conversationId)
                        .header(HttpHeaders.AUTHORIZATION, DocsTestSupport.auth(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].sessionId").value(sessionId))
                .andDo(document("playback-sessions-list",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(
                                headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer {accessToken}")
                        ),
                        pathParameters(
                                parameterWithName("conversationId").description("대화방 ID(UUID)")
                        ),
                        responseFields(
                                fieldWithPath("items").type(ARRAY).description("공동 플레이 세션 목록"),
                                fieldWithPath("items[].sessionId").type(STRING).description("세션 ID"),
                                fieldWithPath("items[].conversationId").type(STRING).description("대화방 ID"),
                                fieldWithPath("items[].hostUserId").type(STRING).description("호스트 사용자 ID"),
                                fieldWithPath("items[].messageId").optional().type(STRING).description("타임라인 세션 메시지 ID"),
                                fieldWithPath("items[].mediaKind").type(STRING).description("미디어 종류"),
                                fieldWithPath("items[].title").type(STRING).description("세션 제목"),
                                fieldWithPath("items[].sourceUrl").type(STRING).description("외부 링크 URL"),
                                fieldWithPath("items[].thumbnailUrl").optional().type(STRING).description("썸네일 URL"),
                                fieldWithPath("items[].status").type(STRING).description("세션 상태(ACTIVE, ENDED)"),
                                fieldWithPath("items[].playbackState").type(STRING).description("재생 상태(PAUSED, PLAYING)"),
                                fieldWithPath("items[].positionSeconds").type(NUMBER).description("현재 재생 위치(초)"),
                                fieldWithPath("items[].startedAt").optional().type(VARIES).description("최초 재생 시작 시각(null 가능)"),
                                fieldWithPath("items[].endedAt").optional().type(VARIES).description("종료 시각(null 가능)"),
                                fieldWithPath("items[].lastControlledAt").optional().type(VARIES).description("마지막 제어 시각(null 가능)"),
                                fieldWithPath("items[].lastControlledBy").optional().type(VARIES).description("마지막 제어 사용자 ID(null 가능)"),
                                fieldWithPath("items[].createdAt").type(STRING).description("생성 시각"),
                                fieldWithPath("items[].participants").type(ARRAY).description("참여자 목록"),
                                fieldWithPath("items[].participants[].userId").type(STRING).description("참여자 사용자 ID"),
                                fieldWithPath("items[].participants[].role").type(STRING).description("참여 역할(HOST, GUEST)"),
                                fieldWithPath("items[].participants[].lastSeenAt").optional().type(VARIES).description("마지막 확인 시각(null 가능)")
                        )
                ));

        PlaybackSessionStateUpdateRequest updateRequest = new PlaybackSessionStateUpdateRequest(PlaybackState.PLAYING, 87);

        mockMvc.perform(patch("/api/conversations/{conversationId}/playback-sessions/{sessionId}/state", conversationId, sessionId)
                        .header(HttpHeaders.AUTHORIZATION, DocsTestSupport.auth(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.playbackState").value("PLAYING"))
                .andExpect(jsonPath("$.positionSeconds").value(87))
                .andDo(document("playback-sessions-update-state",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(
                                headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer {accessToken}")
                        ),
                        pathParameters(
                                parameterWithName("conversationId").description("대화방 ID(UUID)"),
                                parameterWithName("sessionId").description("세션 ID(UUID)")
                        ),
                        requestFields(
                                fieldWithPath("playbackState").type(STRING).description("재생 상태(PAUSED, PLAYING)"),
                                fieldWithPath("positionSeconds").type(NUMBER).description("재생 위치(초)")
                        ),
                        responseFields(sessionResponseFields())
                ));

        mockMvc.perform(post("/api/conversations/{conversationId}/playback-sessions/{sessionId}/end", conversationId, sessionId)
                        .header(HttpHeaders.AUTHORIZATION, DocsTestSupport.auth(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"playbackState\":\"PAUSED\",\"positionSeconds\":91}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ENDED"))
                .andDo(document("playback-sessions-end",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(
                                headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer {accessToken}")
                        ),
                        pathParameters(
                                parameterWithName("conversationId").description("대화방 ID(UUID)"),
                                parameterWithName("sessionId").description("세션 ID(UUID)")
                        ),
                        requestFields(
                                fieldWithPath("playbackState").optional().type(STRING).description("전송해도 무시되는 값(호환용)"),
                                fieldWithPath("positionSeconds").optional().type(NUMBER).description("종료 시 기록할 재생 위치(초)")
                        ),
                        responseFields(sessionResponseFields())
                ));
    }

    private FieldDescriptor[] sessionResponseFields() {
        return new FieldDescriptor[]{
                fieldWithPath("sessionId").type(STRING).description("세션 ID"),
                fieldWithPath("conversationId").type(STRING).description("대화방 ID"),
                fieldWithPath("hostUserId").type(STRING).description("호스트 사용자 ID"),
                fieldWithPath("messageId").optional().type(STRING).description("타임라인 세션 메시지 ID"),
                fieldWithPath("mediaKind").type(STRING).description("미디어 종류"),
                fieldWithPath("title").type(STRING).description("세션 제목"),
                fieldWithPath("sourceUrl").type(STRING).description("외부 링크 URL"),
                fieldWithPath("thumbnailUrl").optional().type(STRING).description("썸네일 URL"),
                fieldWithPath("status").type(STRING).description("세션 상태(ACTIVE, ENDED)"),
                fieldWithPath("playbackState").type(STRING).description("재생 상태(PAUSED, PLAYING)"),
                fieldWithPath("positionSeconds").type(NUMBER).description("현재 재생 위치(초)"),
                fieldWithPath("startedAt").optional().type(VARIES).description("최초 재생 시작 시각(null 가능)"),
                fieldWithPath("endedAt").optional().type(VARIES).description("종료 시각(null 가능)"),
                fieldWithPath("lastControlledAt").optional().type(VARIES).description("마지막 제어 시각(null 가능)"),
                fieldWithPath("lastControlledBy").optional().type(VARIES).description("마지막 제어 사용자 ID(null 가능)"),
                fieldWithPath("createdAt").type(STRING).description("생성 시각"),
                fieldWithPath("participants").type(ARRAY).description("참여자 목록"),
                fieldWithPath("participants[].userId").type(STRING).description("참여자 사용자 ID"),
                fieldWithPath("participants[].role").type(STRING).description("참여 역할(HOST, GUEST)"),
                fieldWithPath("participants[].lastSeenAt").optional().type(VARIES).description("마지막 확인 시각(null 가능)")
        };
    }
}
