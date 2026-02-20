package com.example.backend.service.user;

import com.example.backend.common.PublicUrlBuilder;
import com.example.backend.controller.me.dto.ProfileUpdateRequest;
import com.example.backend.controller.user.dto.ProfileResponse;
import com.example.backend.domain.file.UploadedFile;
import com.example.backend.domain.user.User;
import com.example.backend.exception.BusinessException;
import com.example.backend.exception.ErrorCode;
import com.example.backend.repository.file.UploadedFileRepository;
import com.example.backend.repository.follow.FollowRepository;
import com.example.backend.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserRepository userRepository;
    private final UploadedFileRepository uploadedFileRepository;
    private final FollowRepository followRepository;
    private final PublicUrlBuilder urlBuilder;

    @Transactional(readOnly = true)
    public ProfileResponse getProfileByHandle(String handle) {
        User user = userRepository.findByHandle(handle)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        long followerCount = followRepository.countByFollowingIdAndDeletedFalse(user.getId());
        long followingCount = followRepository.countByFollowerIdAndDeletedFalse(user.getId());

        String profileImageUrl = toFileUrl(user.getProfileImageFileId());

        return new ProfileResponse(
                user.getId(),
                user.getHandle(),
                user.getName(),
                user.getBio(),
                user.getWebsite(),
                profileImageUrl,
                followerCount,
                followingCount
        );
    }

    // ✅ 추가: UUID로 프로필 조회
    @Transactional(readOnly = true)
    public ProfileResponse getProfileById(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        long followerCount = followRepository.countByFollowingIdAndDeletedFalse(user.getId());
        long followingCount = followRepository.countByFollowerIdAndDeletedFalse(user.getId());

        String profileImageUrl = toFileUrl(user.getProfileImageFileId());

        return new ProfileResponse(
                user.getId(),
                user.getHandle(),
                user.getName(),
                user.getBio(),
                user.getWebsite(),
                profileImageUrl,
                followerCount,
                followingCount
        );
    }

    @Transactional
    public ProfileResponse updateMyProfile(UUID meId, ProfileUpdateRequest request) {
        User me = userRepository.findById(meId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        UUID newFileId = request.profileImageFileId();
        if (newFileId != null) {
            UploadedFile file = uploadedFileRepository.findByIdAndDeletedFalse(newFileId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.FILE_NOT_FOUND));

            // 내 파일만 허용
            if (!file.getUploaderId().equals(meId)) {
                throw new BusinessException(ErrorCode.FORBIDDEN);
            }
        }

        me.updateProfile(request.bio(), request.website(), newFileId);

        long followerCount = followRepository.countByFollowingIdAndDeletedFalse(me.getId());
        long followingCount = followRepository.countByFollowerIdAndDeletedFalse(me.getId());

        return new ProfileResponse(
                me.getId(),
                me.getHandle(),
                me.getName(),
                me.getBio(),
                me.getWebsite(),
                toFileUrl(me.getProfileImageFileId()),
                followerCount,
                followingCount
        );
    }

    private String toFileUrl(UUID fileId) {
        if (fileId == null) return null;
        return urlBuilder.absolute("/api/files/" + fileId + "/download");
    }
}