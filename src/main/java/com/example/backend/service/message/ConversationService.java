package com.example.backend.service.message;

import com.example.backend.controller.message.dto.GroupConversationMembersResponse;
import com.example.backend.domain.message.Conversation;
import com.example.backend.domain.message.ConversationMember;
import com.example.backend.domain.message.DirectConversationKey;
import com.example.backend.exception.BusinessException;
import com.example.backend.exception.ErrorCode;
import com.example.backend.repository.message.ConversationMemberRepository;
import com.example.backend.repository.message.ConversationRepository;
import com.example.backend.repository.message.DirectConversationKeyRepository;
import com.example.backend.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final ConversationMemberRepository memberRepository;
    private final DirectConversationKeyRepository directKeyRepository;
    private final UserRepository userRepository;

    @Transactional
    public UUID createOrGetDirect(UUID meId, UUID targetUserId) {
        if (meId.equals(targetUserId)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        userRepository.findById(meId).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        userRepository.findById(targetUserId).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        UUID u1 = (meId.compareTo(targetUserId) <= 0) ? meId : targetUserId;
        UUID u2 = (meId.compareTo(targetUserId) <= 0) ? targetUserId : meId;

        var existing = directKeyRepository.findByUser1IdAndUser2IdAndDeletedFalse(u1, u2);
        if (existing.isPresent()) {
            return existing.get().getConversationId();
        }

        Conversation c = conversationRepository.save(Conversation.direct());

        memberRepository.save(ConversationMember.join(c.getId(), meId));
        memberRepository.save(ConversationMember.join(c.getId(), targetUserId));

        try {
            directKeyRepository.save(DirectConversationKey.of(c.getId(), meId, targetUserId));
            return c.getId();
        } catch (DataIntegrityViolationException e) {
            return directKeyRepository.findByUser1IdAndUser2IdAndDeletedFalse(u1, u2)
                    .map(DirectConversationKey::getConversationId)
                    .orElseThrow(() -> e);
        }
    }

    @Transactional
    public UUID createGroup(UUID meId, String title, List<UUID> participantIds, UUID coverImageFileId) {
        userRepository.findById(meId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Set<UUID> memberIds = new LinkedHashSet<>();
        memberIds.add(meId);

        if (participantIds != null) {
            memberIds.addAll(participantIds);
        }

        if (memberIds.size() < 2) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        for (UUID userId : memberIds) {
            userRepository.findById(userId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        }

        Conversation conversation = conversationRepository.save(
                Conversation.group(title, meId, coverImageFileId)
        );

        for (UUID userId : memberIds) {
            memberRepository.save(ConversationMember.join(conversation.getId(), userId));
        }

        return conversation.getId();
    }


    @Transactional
    public UUID updateGroupInfo(UUID meId, UUID conversationId, String title, UUID coverImageFileId) {
        var conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REQUEST));

        if (conversation.getType() != com.example.backend.domain.message.ConversationType.GROUP) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        if (conversation.getOwnerId() == null || !conversation.getOwnerId().equals(meId)) {
            throw new BusinessException(ErrorCode.MESSAGE_FORBIDDEN);
        }

        conversation.updateGroupInfo(title, coverImageFileId);
        return conversation.getId();
    }

    @Transactional
    public UUID inviteMembers(UUID meId, UUID conversationId, List<UUID> participantIds) {
        var conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REQUEST));

        if (conversation.getType() != com.example.backend.domain.message.ConversationType.GROUP) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        if (conversation.getOwnerId() == null || !conversation.getOwnerId().equals(meId)) {
            throw new BusinessException(ErrorCode.MESSAGE_FORBIDDEN);
        }

        if (participantIds != null) {
            for (UUID userId : participantIds) {
                userRepository.findById(userId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
                if (!memberRepository.existsByConversationIdAndUserId(conversationId, userId)) {
                    memberRepository.save(ConversationMember.join(conversationId, userId));
                }
            }
        }
        return conversationId;
    }

    @Transactional
    public UUID removeMember(UUID meId, UUID conversationId, UUID userId) {
        var conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REQUEST));

        if (conversation.getType() != com.example.backend.domain.message.ConversationType.GROUP) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        if (conversation.getOwnerId() == null || !conversation.getOwnerId().equals(meId)) {
            throw new BusinessException(ErrorCode.MESSAGE_FORBIDDEN);
        }
        if (userId.equals(meId)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        memberRepository.findByConversationIdAndUserId(conversationId, userId)
                .ifPresent(memberRepository::delete);
        return conversationId;
    }

    @Transactional(readOnly = true)
    public GroupConversationMembersResponse getGroupMembers(UUID meId, UUID conversationId) {
        var membership = memberRepository.findByConversationIdAndUserId(conversationId, meId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MESSAGE_FORBIDDEN));

        var conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REQUEST));

        if (conversation.getType() == com.example.backend.domain.message.ConversationType.DIRECT) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        var members = memberRepository.findActiveMembers(conversationId);
        var userIds = members.stream().map(ConversationMember::getUserId).toList();
        var userMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(u -> u.getId(), u -> u));

        return new GroupConversationMembersResponse(
                conversationId,
                members.stream()
                        .map(m -> {
                            var u = userMap.get(m.getUserId());
                            return new GroupConversationMembersResponse.MemberItem(
                                    m.getUserId(),
                                    u != null ? u.getHandle() : null,
                                    u != null ? u.getName() : null,
                                    null
                            );
                        })
                        .toList()
        );
    }
}
