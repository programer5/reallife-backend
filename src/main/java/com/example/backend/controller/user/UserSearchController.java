package com.example.backend.controller.user;

import com.example.backend.controller.user.dto.UserSearchResponse;
import com.example.backend.service.user.UserSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

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
        // 인증 필요(인스타처럼). meId는 지금은 사용 안 하지만 추후 "나 제외" 같은 필터에 씀.
        authentication.getName();
        return userSearchService.search(q, cursor, size);
    }
}