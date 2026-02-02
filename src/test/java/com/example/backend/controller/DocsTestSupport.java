package com.example.backend.controller;

import com.example.backend.domain.post.Post;
import com.example.backend.domain.post.PostVisibility;
import com.example.backend.domain.user.User;
import com.example.backend.repository.post.PostRepository;
import com.example.backend.repository.user.UserRepository;
import com.example.backend.security.JwtTokenProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
public class DocsTestSupport {

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final JwtTokenProvider jwtTokenProvider;

    public DocsTestSupport(UserRepository userRepository,
                           PostRepository postRepository,
                           JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.postRepository = postRepository;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    public static String headerName() {
        return HttpHeaders.AUTHORIZATION;
    }

    public static String auth(String token) {
        return "Bearer " + token;
    }

    @Transactional
    public User saveUser(String prefix, String name) {
        String email = prefix + "+" + UUID.randomUUID() + "@test.com";
        String handle = prefix + "_" + UUID.randomUUID().toString().substring(0, 8);

        // ⚠️ 네 User 생성자 시그니처가 (email, handle, password, name) 맞다는 전제
        User user = new User(email, handle, "encoded", name);

        return userRepository.saveAndFlush(user);
    }

    @Transactional
    public Post savePost(UUID authorId, String content) {
        // ✅ 기본값은 ALL(공개)로
        Post post = Post.create(authorId, content, PostVisibility.ALL);
        return postRepository.saveAndFlush(post);
    }

    public String issueTokenFor(User user) {
        // ✅ subject = userId, claim = email (너 프로젝트 컨벤션)
        return jwtTokenProvider.createAccessToken(user.getId().toString(), user.getEmail());
    }
}
