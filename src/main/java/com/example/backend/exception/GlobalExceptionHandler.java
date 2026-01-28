package com.example.backend.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        ErrorCode code = ErrorCode.VALIDATION_ERROR;

        List<ErrorResponse.FieldError> errors = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::toFieldError)
                .toList();

        ErrorResponse body = new ErrorResponse(
                code.name(),
                code.message(),
                LocalDateTime.now(),
                errors
        );

        return ResponseEntity.status(code.status()).body(body);
    }

    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateEmail(DuplicateEmailException e) {
        ErrorCode code = ErrorCode.DUPLICATE_EMAIL;

        ErrorResponse body = new ErrorResponse(
                code.name(),
                code.message(),
                LocalDateTime.now(),
                List.of()
        );

        return ResponseEntity.status(code.status()).body(body);
    }

    private ErrorResponse.FieldError toFieldError(FieldError fe) {
        String reason = (fe.getDefaultMessage() == null) ? "invalid" : fe.getDefaultMessage();
        return new ErrorResponse.FieldError(fe.getField(), reason);
    }
}