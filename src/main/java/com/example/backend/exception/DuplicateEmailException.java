package com.example.backend.exception;

public class DuplicateEmailException extends RuntimeException {
    public DuplicateEmailException() {
        super(ErrorCode.DUPLICATE_EMAIL.message());
    }
}