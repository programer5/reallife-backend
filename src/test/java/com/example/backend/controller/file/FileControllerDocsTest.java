package com.example.backend.controller.file;

import com.example.backend.controller.DocsTestSupport;
import com.example.backend.repository.file.UploadedFileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
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
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.JsonFieldType.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.partWithName;
import static org.springframework.restdocs.request.RequestDocumentation.requestParts;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@ExtendWith({RestDocumentationExtension.class, SpringExtension.class})
@Transactional
class FileControllerDocsTest {

    @Autowired private WebApplicationContext context;
    @Autowired private DocsTestSupport docs;

    @Autowired private UploadedFileRepository uploadedFileRepository;

    private MockMvc mockMvc(RestDocumentationContextProvider restDocumentation) {
        return MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .apply(documentationConfiguration(restDocumentation))
                .build();
    }

    @Test
    void 파일_업로드_성공_200(RestDocumentationContextProvider restDocumentation) throws Exception {
        MockMvc mockMvc = mockMvc(restDocumentation);

        var me = docs.saveUser("files", "파일유저");
        String token = docs.issueTokenFor(me);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "cat.png",
                "image/png",
                "fake image bytes".getBytes()
        );

        mockMvc.perform(multipart("/api/files")
                        .file(file)
                        .header(DocsTestSupport.headerName(), DocsTestSupport.auth(token))
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileId").exists())
                .andExpect(jsonPath("$.url").exists())
                .andDo(document("files-upload",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(
                                headerWithName(DocsTestSupport.headerName()).description("Bearer {accessToken}")
                        ),
                        requestParts(
                                partWithName("file").description("업로드할 파일(멀티파트)")
                        ),
                        responseFields(
                                fieldWithPath("fileId").type(STRING).description("업로드된 파일 ID(UUID)"),
                                fieldWithPath("mediaType").type(STRING).description("정규화된 미디어 타입(IMAGE, VIDEO, FILE)"),
                                fieldWithPath("url").type(STRING).description("대표 접근 URL"),
                                fieldWithPath("downloadUrl").type(STRING).description("원본 파일 다운로드 URL (예: /api/files/{id}/download)"),
                                fieldWithPath("previewUrl").type(STRING).description("미리보기 URL"),
                                fieldWithPath("thumbnailUrl").optional().type(STRING).description("썸네일 URL (이미지/비디오일 때 제공 가능)"),
                                fieldWithPath("streamingUrl").optional().type(STRING).description("동영상 스트리밍 URL (비디오일 때 제공 가능)"),
                                fieldWithPath("originalFilename").type(STRING).description("원본 파일명"),
                                fieldWithPath("contentType").type(STRING).description("MIME 타입"),
                                fieldWithPath("size").type(NUMBER).description("파일 크기(bytes)"),
                                fieldWithPath("fileType").type(STRING).description("파일 타입(IMAGE, VIDEO, FILE)")
                        )
                ));
    }

    @Test
    void 비디오_업로드_성공_200(RestDocumentationContextProvider restDocumentation) throws Exception {
        MockMvc mockMvc = mockMvc(restDocumentation);

        var me = docs.saveUser("videofile", "비디오유저");
        String token = docs.issueTokenFor(me);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "clip.mp4",
                "video/mp4",
                "fake mp4 bytes".getBytes()
        );

        mockMvc.perform(multipart("/api/files")
                        .file(file)
                        .header(DocsTestSupport.headerName(), DocsTestSupport.auth(token))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileType").value("VIDEO"))
                .andDo(document("files-upload-video",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(
                                headerWithName(DocsTestSupport.headerName()).description("Bearer {accessToken}")
                        ),
                        requestParts(
                                partWithName("file").description("업로드할 비디오 파일(멀티파트)")
                        ),
                        responseFields(
                                fieldWithPath("fileId").type(STRING).description("업로드된 파일 ID(UUID)"),
                                fieldWithPath("mediaType").type(STRING).description("정규화된 미디어 타입(IMAGE, VIDEO, FILE)"),
                                fieldWithPath("url").type(STRING).description("대표 접근 URL"),
                                fieldWithPath("downloadUrl").type(STRING).description("원본 파일 다운로드 URL"),
                                fieldWithPath("previewUrl").type(STRING).description("미리보기 URL"),
                                fieldWithPath("thumbnailUrl").optional().type(STRING).description("비디오 썸네일 URL(가능한 경우)"),
                                fieldWithPath("streamingUrl").optional().type(STRING).description("비디오 스트리밍 URL"),
                                fieldWithPath("originalFilename").type(STRING).description("원본 파일명"),
                                fieldWithPath("contentType").type(STRING).description("MIME 타입"),
                                fieldWithPath("size").type(NUMBER).description("파일 크기(bytes)"),
                                fieldWithPath("fileType").type(STRING).description("파일 타입(IMAGE, VIDEO, FILE)")
                        )
                ));
    }
}
