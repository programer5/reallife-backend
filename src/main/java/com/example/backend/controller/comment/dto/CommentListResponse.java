package com.example.backend.controller.comment.dto;

import org.springframework.data.domain.Page;

import java.util.List;

public record CommentListResponse(
        List<CommentResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static CommentListResponse from(Page<CommentResponse> page) {
        return new CommentListResponse(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}