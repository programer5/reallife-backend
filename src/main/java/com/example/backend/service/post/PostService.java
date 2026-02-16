package com.example.backend.service.post;

import com.example.backend.controller.post.dto.PostCreateRequest;
import com.example.backend.controller.post.dto.PostCreateResponse;
import com.example.backend.domain.post.Post;
import com.example.backend.exception.BusinessException;
import com.example.backend.exception.ErrorCode;
import com.example.backend.repository.file.UploadedFileRepository;
import com.example.backend.repository.post.PostRepository;
import com.example.backend.repository.user.UserRepository;
import com.example.backend.security.ContentSanitizer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final UploadedFileRepository uploadedFileRepository;

    @Transactional
    public PostCreateResponse createPost(UUID meId, PostCreateRequest request) {

        userRepository.findById(meId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        String safeContent = ContentSanitizer.minimal(request.content());
        Post post = Post.create(meId, safeContent, request.visibility());

        // ===== 1. 새 방식 (fileId 기반) =====
        if (request.imageFileIds() != null && !request.imageFileIds().isEmpty()) {

            var files = uploadedFileRepository.findAllById(request.imageFileIds());

            if (files.size() != request.imageFileIds().size()) {
                throw new BusinessException(ErrorCode.FILE_NOT_FOUND);
            }

            int order = 0;
            for (var file : files) {
                post.addImage(file, order++);
            }
        }
        // ===== 2. 구버전 호환 (문자열 URL) =====
        else if (request.imageUrls() != null) {
            for (int i = 0; i < request.imageUrls().size(); i++) {
                post.addImage(request.imageUrls().get(i), i);
            }
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

    public PostCreateResponse getPost(UUID postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));

        return new PostCreateResponse(
                post.getId(),
                post.getAuthorId(),
                post.getContent(),
                post.getImages().stream().map(img -> img.getImageUrl()).toList(),
                post.getVisibility(),
                post.getCreatedAt()
        );
    }

    @Transactional
    public void deletePost(UUID meId, UUID postId) {

        userRepository.findById(meId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        var post = postRepository.findByIdAndAuthorIdAndDeletedFalse(postId, meId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_OWNED));

        post.delete();
    }
}
