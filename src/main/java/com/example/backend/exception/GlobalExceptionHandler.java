package com.example.backend.exception;

import com.example.backend.ops.OpsAlertService;
import com.example.backend.service.error.ErrorLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
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

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final ErrorLogService errorLogService;
    private final OpsAlertService opsAlertService;

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnknown(Exception e, HttpServletRequest req) {
        ErrorCode ec = ErrorCode.INTERNAL_ERROR;

        log.error("Unhandled exception. path={}", req.getRequestURI(), e);

        try {
            errorLogService.record(
                    e.getClass().getSimpleName(),
                    e.getMessage(),
                    req.getRequestURI()
            );
        } catch (Exception logEx) {
            log.error("Failed to persist error log. path={}", req.getRequestURI(), logEx);
        }

        try {
            opsAlertService.sendUnhandledExceptionAlert(e, req);
        } catch (Exception alertEx) {
            log.error("Failed to send ops alert. path={}", req.getRequestURI(), alertEx);
        }

        return ResponseEntity.status(ec.status()).body(
                new ErrorResponse(
                        ec.code(),
                        e.getMessage(),
                        LocalDateTime.now(),
                        req.getRequestURI(),
                        null
                )
        );
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException e, HttpServletRequest req) {
        ErrorCode ec = e.getErrorCode();

        return ResponseEntity.status(ec.status()).body(
                new ErrorResponse(
                        ec.code(),
                        e.getMessage(),
                        LocalDateTime.now(),
                        req.getRequestURI(),
                        null
                )
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
        String uri = request.getRequestURI();

        boolean isSse = uri != null && uri.startsWith("/api/sse");
        boolean isEventStream = response.getContentType() != null
                && response.getContentType().contains(MediaType.TEXT_EVENT_STREAM_VALUE);

        if (isSse || isEventStream) {
            return;
        }

        throw new RuntimeException(e);
    }

    private ErrorResponse.FieldError toFieldError(FieldError fe) {
        return new ErrorResponse.FieldError(fe.getField(), fe.getDefaultMessage());
    }
}