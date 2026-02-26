package com.example.backend.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    // Common
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "COMMON_INVALID_REQUEST", "요청이 올바르지 않습니다."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_INTERNAL_ERROR", "서버 오류가 발생했습니다."),

    // Auth
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "AUTH_UNAUTHORIZED", "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "AUTH_FORBIDDEN", "권한이 없습니다."),
    TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, "AUTH_TOO_MANY_REQUESTS", "요청이 너무 많습니다. 잠시 후 다시 시도해주세요."),

    // User
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "사용자를 찾을 수 없습니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "USER_DUPLICATE_EMAIL", "이미 가입된 이메일입니다."),
    DUPLICATE_HANDLE(HttpStatus.CONFLICT, "USER_DUPLICATE_HANDLE", "이미 사용 중인 아이디입니다."),

    // Post
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "POST_NOT_FOUND", "게시글을 찾을 수 없습니다."),
    POST_NOT_OWNED(HttpStatus.FORBIDDEN, "POST_NOT_OWNED", "본인 게시글만 수정/삭제할 수 있습니다."),

    // Follow
    CANNOT_FOLLOW_SELF(HttpStatus.BAD_REQUEST, "FOLLOW_CANNOT_FOLLOW_SELF", "자기 자신을 팔로우할 수 없습니다."),

    // Like
    LIKE_ALREADY_EXISTS(HttpStatus.CONFLICT, "LIKE_ALREADY_EXISTS", "이미 좋아요한 게시글입니다."),

    // Message
    MESSAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "MESSAGE_NOT_FOUND", "메시지를 찾을 수 없습니다."),
    MESSAGE_CONVERSATION_NOT_FOUND(HttpStatus.NOT_FOUND, "MESSAGE_CONVERSATION_NOT_FOUND", "대화방을 찾을 수 없습니다."),
    MESSAGE_FORBIDDEN(HttpStatus.FORBIDDEN, "MESSAGE_FORBIDDEN", "대화방에 접근할 수 없습니다."),
    MESSAGE_EMPTY(HttpStatus.BAD_REQUEST, "MESSAGE_EMPTY", "내용 또는 첨부파일 중 하나는 필수입니다."),

    // Conversation
    CONVERSATION_NOT_FOUND(HttpStatus.NOT_FOUND, "MESSAGE_CONVERSATION_NOT_FOUND", "대화방을 찾을 수 없습니다."),
    CONVERSATION_LOCKED(HttpStatus.LOCKED, "CONVERSATION_LOCKED", "잠금된 대화입니다."),
    CONVERSATION_LOCK_PASSWORD_INVALID(HttpStatus.FORBIDDEN, "CONVERSATION_LOCK_PASSWORD_INVALID", "비밀번호가 올바르지 않습니다."),

    PIN_NOT_FOUND(HttpStatus.NOT_FOUND, "PIN_NOT_FOUND", "핀을 찾을 수 없습니다."),

    // File
    FILE_TOO_LARGE(HttpStatus.BAD_REQUEST, "FILE_TOO_LARGE", "파일 크기가 너무 큽니다."),
    FILE_TYPE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "FILE_TYPE_NOT_ALLOWED", "허용되지 않는 파일 형식입니다."),
    FILE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "FILE_UPLOAD_FAILED", "파일 업로드에 실패했습니다."),
    FILE_NOT_FOUND(HttpStatus.NOT_FOUND, "FILE_NOT_FOUND", "파일을 찾을 수 없습니다."),

    // Notification
    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "NOTIFICATION_NOT_FOUND", "알림을 찾을 수 없습니다."),

    // Comment
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMENT_NOT_FOUND", "댓글을 찾을 수 없습니다."),
    COMMENT_NOT_OWNED(HttpStatus.FORBIDDEN, "COMMENT_NOT_OWNED", "본인 댓글만 삭제할 수 있습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    public HttpStatus status() { return status; }
    public String code() { return code; }
    public String message() { return message; }
}