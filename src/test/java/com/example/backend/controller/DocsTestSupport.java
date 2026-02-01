package com.example.backend.controller;

import com.example.backend.domain.user.User;
import com.example.backend.repository.user.UserRepository;
import com.example.backend.security.JwtTokenProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
public class DocsTestSupport {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    public DocsTestSupport(UserRepository userRepository, JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Transactional
    public User saveUser(String prefix, String name) {
        // ✅ email 길이 폭발 방지: UUID 일부만 사용
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String email = prefix + "+" + suffix + "@t.com"; // 짧게!
        // ✅ handle도 20자 제한 대비(있을 수 있음)
        String handle = (prefix + "_" + suffix);
        if (handle.length() > 20) {
            handle = handle.substring(0, 20);
        }
        return userRepository.saveAndFlush(new User(email, handle, "encoded", name));
    }

    public String issueTokenFor(User user) {
        String token = jwtTokenProvider.createAccessToken(user.getId().toString(), user.getEmail());
        return "Bearer " + token;
    }

    public static String headerName() {
        return HttpHeaders.AUTHORIZATION;
    }
}
