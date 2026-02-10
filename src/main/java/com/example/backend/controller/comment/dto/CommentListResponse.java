package com.example.backend.controller.comment.dto;

import java.util.List;

public record CommentListResponse(
        List<CommentListItem> items,
        String nextCursor,
        boolean hasNext
) {}