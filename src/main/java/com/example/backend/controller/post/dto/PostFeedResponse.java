package com.example.backend.controller.post.dto;

import java.util.List;

public record PostFeedResponse(
        List<PostFeedItem> items,
        String nextCursor,   // 다음 페이지 요청에 그대로 넣을 값 (없으면 null)
        boolean hasNext
) {
}
