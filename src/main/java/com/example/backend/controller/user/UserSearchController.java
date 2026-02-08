package com.example.backend.controller.user;

import com.example.backend.controller.user.dto.UserSearchResponse;
import com.example.backend.exception.BusinessException;
import com.example.backend.exception.ErrorCode;
import com.example.backend.service.user.UserSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
        // ✅ q blank -> 400
        if (q == null || q.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        // ✅ size 범위 -> 400 (테스트가 999로 400 기대하니까 normalize 하지 말고 막아주자)
        if (size != null && (size < 1 || size > 50)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        UUID meId = UUID.fromString(authentication.getName());
        return userSearchService.search(meId, q, cursor, size);
    }
}