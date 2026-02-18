package com.example.backend.controller.file;

import com.example.backend.controller.DocsTestSupport;
import com.example.backend.domain.file.UploadedFile;
import com.example.backend.repository.file.UploadedFileRepository;
import com.example.backend.service.file.StorageService;
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
import org.springframework.web.context.WebApplicationContext;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.restdocs.headers.HeaderDocumentation.*;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@ExtendWith({RestDocumentationExtension.class, SpringExtension.class})
class FileServeControllerDocsTest {

    @Autowired private WebApplicationContext context;
    @Autowired private DocsTestSupport docs;
    @Autowired private UploadedFileRepository uploadedFileRepository;
    @Autowired private StorageService storageService;

    private MockMvc mockMvc(RestDocumentationContextProvider restDocumentation) {
        return MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .apply(documentationConfiguration(restDocumentation))
                .build();
    }

    @Test
    void 파일_다운로드_200_및_304(RestDocumentationContextProvider restDocumentation) throws Exception {
        MockMvc mockMvc = mockMvc(restDocumentation);

        var me = docs.saveUser("files", "파일유저");
        String token = docs.issueTokenFor(me);

        // DB row 만들기
        String fileKey = "test-download.bin";
        UploadedFile saved = uploadedFileRepository.saveAndFlush(
                UploadedFile.create(me.getId(), "hello.bin", fileKey, "application/octet-stream", 5)
        );

        // 실제 파일 쓰기
        Path p = storageService.resolvePath(fileKey);
        Files.createDirectories(p.getParent());
        Files.write(p, "hello".getBytes());

        // 첫 호출 200 + ETag 확인
        var result = mockMvc.perform(get("/api/files/{fileId}/download", saved.getId())
                        // download가 permitAll이어도, 있어도 무해 / 정책 바뀌어도 안전
                        .header(DocsTestSupport.headerName(), DocsTestSupport.auth(token)))
                .andExpect(status().isOk())
                .andExpect(header().exists(HttpHeaders.ETAG))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("immutable")))
                .andDo(document("files-download-200",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                                parameterWithName("fileId").description("파일 ID(UUID)")
                        ),
                        responseHeaders(
                                headerWithName(HttpHeaders.CONTENT_TYPE).description("Content-Type"),
                                headerWithName(HttpHeaders.CONTENT_DISPOSITION).description("inline 또는 attachment"),
                                headerWithName(HttpHeaders.CACHE_CONTROL).description("public, max-age=31536000, immutable"),
                                headerWithName(HttpHeaders.ETAG).description("ETag")
                        )
                ))
                .andReturn();

        String etag = result.getResponse().getHeader(HttpHeaders.ETAG);

        // If-None-Match로 304
        mockMvc.perform(get("/api/files/{fileId}/download", saved.getId())
                        .header(HttpHeaders.IF_NONE_MATCH, etag)
                        .header(DocsTestSupport.headerName(), DocsTestSupport.auth(token)))
                .andExpect(status().isNotModified())
                .andDo(document("files-download-304",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(parameterWithName("fileId").description("파일 ID(UUID)")),
                        requestHeaders(
                                headerWithName(HttpHeaders.IF_NONE_MATCH).description("ETag 값")
                        ),
                        responseHeaders(
                                headerWithName(HttpHeaders.ETAG).description("ETag")
                        )
                ));
    }

    @Test
    void 썸네일_200_및_304(RestDocumentationContextProvider restDocumentation) throws Exception {
        MockMvc mockMvc = mockMvc(restDocumentation);

        var me = docs.saveUser("thumb", "썸유저");
        String token = docs.issueTokenFor(me);

        // 비동기/이벤트 생성 기다리지 않고, "썸네일이 이미 있는 상태"를 직접 구성
        String originalKey = "test-img.jpg";
        String thumbKey = "test-thumb.jpg";

        UploadedFile saved = UploadedFile.create(me.getId(), "pic.jpg", originalKey, "image/jpeg", 3);
        saved.attachThumbnail(thumbKey, "image/jpeg", 4);
        saved = uploadedFileRepository.saveAndFlush(saved);

        // 원본/썸네일 파일 쓰기 (내용은 더미여도 OK)
        Path op = storageService.resolvePath(originalKey);
        Files.createDirectories(op.getParent());
        Files.write(op, new byte[]{1,2,3});

        Path tp = storageService.resolvePath(thumbKey);
        Files.createDirectories(tp.getParent());
        Files.write(tp, new byte[]{9,8,7,6});

        var result = mockMvc.perform(get("/api/files/{fileId}/thumbnail", saved.getId())
                        // thumbnail은 현재 보안 설정상 인증 필요 가능성이 높아서 토큰 포함
                        .header(DocsTestSupport.headerName(), DocsTestSupport.auth(token)))
                .andExpect(status().isOk())
                .andExpect(header().exists(HttpHeaders.ETAG))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("immutable")))
                .andDo(document("files-thumbnail-200",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(parameterWithName("fileId").description("파일 ID(UUID)")),
                        responseHeaders(
                                headerWithName(HttpHeaders.CONTENT_TYPE).description("Content-Type"),
                                headerWithName(HttpHeaders.CONTENT_DISPOSITION).description("inline"),
                                headerWithName(HttpHeaders.CACHE_CONTROL).description("public, max-age=31536000, immutable"),
                                headerWithName(HttpHeaders.ETAG).description("ETag")
                        )
                ))
                .andReturn();

        String etag = result.getResponse().getHeader(HttpHeaders.ETAG);

        mockMvc.perform(get("/api/files/{fileId}/thumbnail", saved.getId())
                        .header(HttpHeaders.IF_NONE_MATCH, etag)
                        .header(DocsTestSupport.headerName(), DocsTestSupport.auth(token)))
                .andExpect(status().isNotModified())
                .andDo(document("files-thumbnail-304",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(parameterWithName("fileId").description("파일 ID(UUID)")),
                        requestHeaders(
                                headerWithName(HttpHeaders.IF_NONE_MATCH).description("ETag 값")
                        ),
                        responseHeaders(
                                headerWithName(HttpHeaders.ETAG).description("ETag")
                        )
                ));
    }
}