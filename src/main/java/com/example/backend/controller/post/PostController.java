package com.example.backend.controller.post;

import com.example.backend.controller.post.dto.PostCreateRequest;
import com.example.backend.controller.post.dto.PostCreateResponse;
import com.example.backend.controller.post.dto.PostFeedResponse;
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
        // JwtAuthenticationFilter에서 principal을 email로 넣고 있으므로 getName() == email
        String email = authentication.getName();
        return postService.createPost(email, request);
    }

    @GetMapping("/{postId}")
    public PostCreateResponse get(@PathVariable UUID postId) {
        return postService.getPost(postId);
    }

    @GetMapping
    public PostFeedResponse feed(
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication
    ) {
        return postService.getFeed(authentication.getName(), cursor, size);
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> delete(@PathVariable UUID postId, Authentication authentication) {
        String email = authentication.getName();
        postService.deletePost(email, postId);
        return ResponseEntity.noContent().build(); // 204
    }


}
