
package com.example.backend.service.post;

import com.example.backend.controller.post.dto.PostCreateRequest;
import com.example.backend.controller.post.dto.PostCreateResponse;
import com.example.backend.controller.post.dto.PostUpdateRequest;
import com.example.backend.domain.post.Post;
import com.example.backend.domain.post.PostImage;
import com.example.backend.exception.BusinessException;
import com.example.backend.exception.ErrorCode;
import com.example.backend.repository.file.UploadedFileRepository;
import com.example.backend.repository.like.PostLikeRepository;
import com.example.backend.repository.post.PostRepository;
import com.example.backend.repository.user.UserRepository;
import com.example.backend.security.ContentSanitizer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final UploadedFileRepository uploadedFileRepository;
    private final PostLikeRepository postLikeRepository;

    @Transactional
    public PostCreateResponse createPost(UUID meId, PostCreateRequest request) {

        userRepository.findById(meId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        String safeContent = ContentSanitizer.minimal(request.content());
        Post post = Post.create(meId, safeContent, request.visibility());

        List<UUID> fileIds = (request.mediaFileIds() != null && !request.mediaFileIds().isEmpty())
                ? request.mediaFileIds()
                : request.imageFileIds();

        if (fileIds != null && !fileIds.isEmpty()) {
            var files = uploadedFileRepository.findAllById(fileIds);

            if (files.size() != fileIds.size()) {
                throw new BusinessException(ErrorCode.FILE_NOT_FOUND);
            }

            int order = 0;
            for (var file : files) {
                post.addMedia(file, order++);
            }
        } else if (request.imageUrls() != null) {
            for (int i = 0; i < request.imageUrls().size(); i++) {
                post.addImage(request.imageUrls().get(i), i);
            }
        }

        Post saved = postRepository.save(post);

        var author = userRepository.findById(saved.getAuthorId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        return new PostCreateResponse(
                saved.getId(),
                saved.getAuthorId(),
                author.getHandle(),
                author.getName(),
                saved.getContent(),
                saved.getImages().stream()
                        .filter(img -> String.valueOf(img.getMediaType()).equals("IMAGE"))
                        .map(PostImage::getImageUrl)
                        .toList(),
                saved.getImages().stream()
                        .map(img -> new PostCreateResponse.MediaItem(
                                img.getMediaType().name(),
                                img.getImageUrl(),
                                img.getThumbnailUrl(),
                                img.getContentType()
                        ))
                        .toList(),
                saved.getVisibility(),
                saved.getCreatedAt(),
                saved.getLikeCount(),
                saved.getCommentCount(),
                false
        );
    }

    @Transactional
    public PostCreateResponse updatePost(UUID meId, UUID postId, PostUpdateRequest request) {

        userRepository.findById(meId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        var post = postRepository.findByIdAndAuthorIdAndDeletedFalse(postId, meId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_OWNED));

        post.update(ContentSanitizer.minimal(request.content()), request.visibility());

        var author = userRepository.findById(post.getAuthorId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        boolean likedByMe = postLikeRepository.findByPostIdAndUserId(postId, meId).isPresent();

        return new PostCreateResponse(
                post.getId(),
                post.getAuthorId(),
                author.getHandle(),
                author.getName(),
                post.getContent(),
                post.getImages().stream()
                        .filter(img -> String.valueOf(img.getMediaType()).equals("IMAGE"))
                        .map(PostImage::getImageUrl)
                        .toList(),
                post.getImages().stream()
                        .map(img -> new PostCreateResponse.MediaItem(
                                img.getMediaType().name(),
                                img.getImageUrl(),
                                img.getThumbnailUrl(),
                                img.getContentType()
                        ))
                        .toList(),
                post.getVisibility(),
                post.getCreatedAt(),
                post.getLikeCount(),
                post.getCommentCount(),
                likedByMe
        );
    }

    public PostCreateResponse getPost(UUID postId, UUID meId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));

        var author = userRepository.findById(post.getAuthorId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        boolean likedByMe = meId != null && postLikeRepository.findByPostIdAndUserId(postId, meId).isPresent();

        return new PostCreateResponse(
                post.getId(),
                post.getAuthorId(),
                author.getHandle(),
                author.getName(),
                post.getContent(),
                post.getImages().stream()
                        .filter(img -> String.valueOf(img.getMediaType()).equals("IMAGE"))
                        .map(PostImage::getImageUrl)
                        .toList(),
                post.getImages().stream()
                        .map(img -> new PostCreateResponse.MediaItem(
                                img.getMediaType().name(),
                                img.getImageUrl(),
                                img.getThumbnailUrl(),
                                img.getContentType()
                        ))
                        .toList(),
                post.getVisibility(),
                post.getCreatedAt(),
                post.getLikeCount(),
                post.getCommentCount(),
                likedByMe
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
