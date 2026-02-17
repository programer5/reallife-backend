package com.example.backend.controller.me;

import com.example.backend.controller.DocsTestSupport;
import com.example.backend.domain.file.UploadedFile;
import com.example.backend.repository.file.UploadedFileRepository;
import com.example.backend.restdocs.ErrorResponseSnippet;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
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

import java.util.HashMap;

import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.JsonFieldType.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.snippet.Attributes.key;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@ExtendWith({RestDocumentationExtension.class, SpringExtension.class})
@Transactional
class MeControllerDocsTest {

    @Autowired WebApplicationContext context;
    @Autowired ObjectMapper objectMapper;
    @Autowired DocsTestSupport docs;
    @Autowired UploadedFileRepository uploadedFileRepository;

    private MockMvc mockMvc(RestDocumentationContextProvider restDocumentation) {
        return MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .apply(documentationConfiguration(restDocumentation))
                .build();
    }

    @Test
    void 내정보_조회_실패_토큰없음_401(RestDocumentationContextProvider restDocumentation) throws Exception {
        mockMvc(restDocumentation)
                .perform(get("/api/me"))
                .andExpect(status().isUnauthorized())
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andDo(document("me-401-unauthorized",
                        responseFields(ErrorResponseSnippet.common())
                ));
    }

    @Test
    void 내정보_조회_성공_토큰있음_200(RestDocumentationContextProvider restDocumentation) throws Exception {
        var me = docs.saveUser("me", "시드유저");
        String token = docs.issueTokenFor(me);

        mockMvc(restDocumentation)
                .perform(get("/api/me")
                        .header(HttpHeaders.AUTHORIZATION, DocsTestSupport.auth(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.email").exists())
                .andExpect(jsonPath("$.handle").exists())
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andDo(document("me-200-ok",
                        requestHeaders(
                                headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer {accessToken}")
                        ),
                        responseFields(
                                fieldWithPath("id").type(STRING).description("내 사용자 ID(UUID)"),
                                fieldWithPath("email").type(STRING).description("내 이메일"),
                                fieldWithPath("handle").type(STRING).description("내 아이디(핸들)"),
                                fieldWithPath("name").type(STRING).description("내 이름"),
                                fieldWithPath("followerCount").type(NUMBER).description("팔로워 수"),
                                fieldWithPath("tier").type(STRING).description("팔로워 티어")
                        )
                ));
    }

    @Test
    void 내프로필_수정_성공_200(RestDocumentationContextProvider restDocumentation) throws Exception {
        MockMvc mockMvc = mockMvc(restDocumentation);

        var me = docs.saveUser("meprofile", "나");
        String token = docs.issueTokenFor(me);

        var req = new HashMap<String, Object>();
        req.put("bio", "안녕하세요");
        req.put("website", "https://example.com");
        req.put("profileImageFileId", null); // 업로드 파일 연결은 FileControllerDocsTest에서 문서화

        mockMvc.perform(patch("/api/me/profile")
                        .header(DocsTestSupport.headerName(), DocsTestSupport.auth(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.handle").exists())
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andDo(document("me-profile-update",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(
                                headerWithName(DocsTestSupport.headerName()).description("Bearer {accessToken}")
                        ),
                        requestFields(
                                fieldWithPath("bio").optional().type(STRING)
                                        .attributes(key("constraints").value("max 255"))
                                        .description("소개"),
                                fieldWithPath("website").optional().type(STRING)
                                        .attributes(key("constraints").value("max 255"))
                                        .description("웹사이트"),
                                fieldWithPath("profileImageFileId").optional().type(STRING)
                                        .description("프로필 이미지 파일 ID(UUID). 없으면 null")
                        ),
                        responseFields(
                                fieldWithPath("id").type(STRING).description("유저 ID(UUID)"),
                                fieldWithPath("handle").type(STRING).description("핸들"),
                                fieldWithPath("name").type(STRING).description("이름"),
                                fieldWithPath("bio").optional().type(STRING).description("소개"),
                                fieldWithPath("website").optional().type(STRING).description("웹사이트"),
                                fieldWithPath("profileImageUrl").optional().type(STRING).description("프로필 이미지 URL(/api/files/{id}/download)"),
                                fieldWithPath("followerCount").type(NUMBER).description("팔로워 수"),
                                fieldWithPath("followingCount").type(NUMBER).description("팔로잉 수")
                        )
                ));
    }

    @Test
    @DisplayName("내 프로필 수정 실패 - 다른 유저 파일 사용(403)")
    void 내프로필_수정_실패_다른유저파일_403(RestDocumentationContextProvider restDocumentation) throws Exception {

        MockMvc mockMvc = mockMvc(restDocumentation);

        // 내 계정
        var me = docs.saveUser("me403", "나");
        String token = docs.issueTokenFor(me);

        // 다른 유저
        var other = docs.saveUser("other403", "남");

        // 다른 유저가 업로드한 파일 생성
        UploadedFile otherFile = UploadedFile.create(
                other.getId(),
                "forbidden.png",
                "docs/forbidden.png",
                "image/png",
                10L
        );
        uploadedFileRepository.saveAndFlush(otherFile);

        var req = new HashMap<String, Object>();
        req.put("bio", "변경시도");
        req.put("website", "https://hack.com");
        req.put("profileImageFileId", otherFile.getId().toString());

        mockMvc.perform(patch("/api/me/profile")
                        .header(DocsTestSupport.headerName(), DocsTestSupport.auth(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden())
                .andDo(document("me-profile-update-403-forbidden-file",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        responseFields(
                                ErrorResponseSnippet.common()
                        )
                ));
    }
}
