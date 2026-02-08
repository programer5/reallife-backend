package com.example.backend.controller.user;

import com.example.backend.controller.user.dto.UserSearchResponse;
import com.example.backend.exception.BusinessException;
import com.example.backend.exception.ErrorCode;
import com.example.backend.service.user.UserSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserSearchController {

    private final UserSearchService userSearchService;

    @GetMapping("/search")
    public UserSearchResponse search(
            @RequestParam String q,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) Integer size,
            Authentication authentication
    ) {
        if (q == null || q.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST); // 아래 ErrorCode 추가/사용
        }
        if (size != null && (size < 1 || size > 50)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        UUID meId = UUID.fromString(authentication.getName());
        return userSearchService.search(meId, q, cursor, size);
    }
}