package com.example.backend.service.post;

import com.example.backend.controller.post.dto.PostCreateRequest;
import com.example.backend.controller.post.dto.PostCreateResponse;
import com.example.backend.domain.post.Post;
import com.example.backend.repository.post.PostRepository;
import com.example.backend.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;

    @Transactional
    public PostCreateResponse createPost(String email, PostCreateRequest request) {
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        UUID authorId = user.getId();
        Post post = Post.create(authorId, request.content(), request.visibility());

        List<String> urls = request.imageUrls() == null ? Collections.emptyList() : request.imageUrls();
        for (int i = 0; i < urls.size(); i++) {
            post.addImage(urls.get(i), i);
        }

        Post saved = postRepository.save(post);

        return new PostCreateResponse(
                saved.getId(),
                saved.getAuthorId(),
                saved.getContent(),
                saved.getImages().stream().map(img -> img.getImageUrl()).toList(),
                saved.getVisibility(),
                saved.getCreatedAt()
        );
    }
}
