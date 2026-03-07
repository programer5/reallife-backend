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
    public ProfileResponse getProfileByHandle(String handle, UUID meId) {
        User user = userRepository.findByHandle(handle)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        return toProfile(user, meId);
    }

    @Transactional(readOnly = true)
    public ProfileResponse getProfileById(UUID userId, UUID meId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        return toProfile(user, meId);
    }

    @Transactional
    public ProfileResponse updateMyProfile(UUID meId, ProfileUpdateRequest request) {
        User me = userRepository.findById(meId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        UUID newFileId = request.profileImageFileId();
        if (newFileId != null) {
            UploadedFile file = uploadedFileRepository.findByIdAndDeletedFalse(newFileId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.FILE_NOT_FOUND));
            if (!file.getUploaderId().equals(meId)) {
                throw new BusinessException(ErrorCode.FORBIDDEN);
            }
        }

        me.updateProfile(request.name(), request.bio(), request.website(), newFileId);
        return toProfile(me, meId);
    }

    private ProfileResponse toProfile(User user, UUID meId) {
        long followerCount = followRepository.countByFollowingIdAndDeletedFalse(user.getId());
        long followingCount = followRepository.countByFollowerIdAndDeletedFalse(user.getId());
        boolean followedByMe = meId != null && !meId.equals(user.getId())
                && followRepository.existsByFollowerIdAndFollowingIdAndDeletedFalse(meId, user.getId());

        return new ProfileResponse(
                user.getId(),
                user.getHandle(),
                user.getName(),
                user.getBio(),
                user.getWebsite(),
                toFileUrl(user.getProfileImageFileId()),
                followerCount,
                followingCount,
                followedByMe
        );
    }

    private String toFileUrl(UUID fileId) {
        if (fileId == null) return null;
        return urlBuilder.absolute("/api/files/" + fileId + "/download");
    }
}
