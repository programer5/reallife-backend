package com.example.backend.restdocs;

import org.springframework.restdocs.payload.FieldDescriptor;

import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;

public final class ErrorResponseSnippet {
    private ErrorResponseSnippet() {}

    public static FieldDescriptor[] common() {
        return new FieldDescriptor[] {
                fieldWithPath("code").description("에러 코드"),
                fieldWithPath("message").description("에러 메시지"),
                fieldWithPath("timestamp").description("에러 발생 시각"),
                fieldWithPath("path").description("요청 경로"),
                fieldWithPath("fieldErrors").optional().description("필드 검증 오류 목록 (검증 오류일 때만 존재)")
        };
    }

    public static FieldDescriptor[] validation() {
        return new FieldDescriptor[] {
                fieldWithPath("code").description("에러 코드"),
                fieldWithPath("message").description("에러 메시지"),
                fieldWithPath("timestamp").description("에러 발생 시각"),
                fieldWithPath("path").description("요청 경로"),
                fieldWithPath("fieldErrors").description("필드 검증 오류 목록"),
                fieldWithPath("fieldErrors[].field").description("오류가 발생한 필드명"),
                fieldWithPath("fieldErrors[].reason").description("오류 사유")
        };
    }
}