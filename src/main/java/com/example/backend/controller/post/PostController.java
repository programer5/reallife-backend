package com.example.backend.controller.post;

import com.example.backend.controller.post.dto.PostCreateRequest;
import com.example.backend.controller.post.dto.PostCreateResponse;
import com.example.backend.service.post.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/posts")
public class PostController {

    private final PostService postService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PostCreateResponse create(@Valid @RequestBody PostCreateRequest request,
                                     Authentication authentication) {
        UUID meId = UUID.fromString(authentication.getName()); // ✅ principal=userId
        return postService.createPost(meId, request);
    }

    @GetMapping("/{postId}")
    public PostCreateResponse get(@PathVariable UUID postId) {
        return postService.getPost(postId);
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> delete(@PathVariable UUID postId, Authentication authentication) {
        UUID meId = UUID.fromString(authentication.getName());
        postService.deletePost(meId, postId);
        return ResponseEntity.noContent().build(); // 204
    }
}
