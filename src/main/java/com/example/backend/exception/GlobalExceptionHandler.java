package com.example.backend.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j // ✅ 추가
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnknown(Exception e, HttpServletRequest req) {
        ErrorCode ec = ErrorCode.INTERNAL_ERROR;

        // ✅ 이 1줄이 없어서 스택트레이스가 안 보였던 거야
        log.error("Unhandled exception. path={}", req.getRequestURI(), e);

        return ResponseEntity.status(ec.status()).body(
                new ErrorResponse(ec.code(), e.getMessage(), LocalDateTime.now(), req.getRequestURI(), null)
        );
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException e, HttpServletRequest req) {
        ErrorCode ec = e.getErrorCode();

        return ResponseEntity.status(ec.status()).body(
                new ErrorResponse(ec.code(), e.getMessage(), LocalDateTime.now(), req.getRequestURI(), null)
        );

    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e, HttpServletRequest req) {
        ErrorCode ec = ErrorCode.INVALID_REQUEST;

        List<ErrorResponse.FieldError> fieldErrors = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ErrorResponse.FieldError(fe.getField(), fe.getDefaultMessage()))
                .toList();

        return ResponseEntity.status(ec.status()).body(
                new ErrorResponse(
                        ec.code(),
                        ec.message(),
                        LocalDateTime.now(),
                        req.getRequestURI(),
                        fieldErrors
                )
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException e, HttpServletRequest req) {
        ErrorCode ec = ErrorCode.INVALID_REQUEST;

        return ResponseEntity.status(ec.status()).body(
                new ErrorResponse(
                        ec.code(),
                        e.getMessage() != null ? e.getMessage() : ec.message(),
                        LocalDateTime.now(),
                        req.getRequestURI(),
                        null
                )
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException e,
            HttpServletRequest req
    ) {
        ErrorCode ec = ErrorCode.INVALID_REQUEST;

        // e.getMessage()는 "Invalid UUID string: 1" 같이 내부 구현 메시지가 섞여서 길 수 있음
        // → message는 공통 메시지로 통일하는 게 운영/보안/문서 측면에서 좋음
        return ResponseEntity.status(ec.status()).body(
                new ErrorResponse(
                        ec.code(),
                        ec.message(),
                        LocalDateTime.now(),
                        req.getRequestURI(),
                        null
                )
        );
    }

    @ExceptionHandler({
            AsyncRequestNotUsableException.class,
            IOException.class
    })
    public void ignoreSseDisconnect(Exception e, HttpServletRequest request, HttpServletResponse response) {
        // SSE 구독 중 클라이언트 끊김은 정상 케이스. 에러 응답 만들지 않음.
        String uri = request.getRequestURI();

        boolean isSse = uri != null && uri.startsWith("/api/sse");
        boolean isEventStream = response.getContentType() != null
                && response.getContentType().contains(MediaType.TEXT_EVENT_STREAM_VALUE);

        if (isSse || isEventStream) {
            // 아무것도 반환하지 않고 조용히 종료 (로그도 INFO/ERROR 대신 DEBUG 권장)
            return;
        }

        // SSE가 아닌 경우에는 기존 unknown handler로 보내거나,
        // 여기서는 그냥 던져서 기존 흐름 유지해도 됨(프로젝트 스타일에 맞춰 선택)
        throw new RuntimeException(e);
    }

    private ErrorResponse.FieldError toFieldError(FieldError fe) {
        return new ErrorResponse.FieldError(fe.getField(), fe.getDefaultMessage());
    }
}
