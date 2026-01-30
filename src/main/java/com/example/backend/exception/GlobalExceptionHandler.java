package com.example.backend.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

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

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnknown(Exception e, HttpServletRequest req) {
        ErrorCode ec = ErrorCode.INTERNAL_ERROR;

        return ResponseEntity.status(ec.status()).body(
                new ErrorResponse(ec.code(), e.getMessage(), LocalDateTime.now(), req.getRequestURI(), null)
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

    private ErrorResponse.FieldError toFieldError(FieldError fe) {
        return new ErrorResponse.FieldError(fe.getField(), fe.getDefaultMessage());
    }
}
