package com.example.backend.service.message;

import com.example.backend.controller.message.dto.MessageCapsuleListResponse;
import com.example.backend.domain.message.MessageCapsule;
import com.example.backend.exception.BusinessException;
import com.example.backend.exception.ErrorCode;
import com.example.backend.repository.message.ConversationMemberRepository;
import com.example.backend.repository.message.MessageCapsuleRepository;
import com.example.backend.repository.message.MessageRepository;
import com.example.backend.search.index.SearchIndexingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MessageCapsuleService {

    private final MessageCapsuleRepository repository;
    private final ConversationMemberRepository conversationMemberRepository;
    private final MessageRepository messageRepository;
    private final SearchIndexingService searchIndexingService;

    @Transactional
    public UUID create(UUID messageId, UUID conversationId, UUID creatorId, String title, LocalDateTime unlockAt) {
        validateConversationAccess(conversationId, creatorId);
        if (unlockAt == null || !unlockAt.isAfter(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "열릴 시간은 현재보다 이후여야 합니다.");
        }

        var message = messageRepository.findByIdAndDeletedFalse(messageId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MESSAGE_NOT_FOUND));
        if (!conversationId.equals(message.getConversationId())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "메시지와 대화방 정보가 일치하지 않습니다.");
        }

        String safeTitle = sanitizeTitle(title, message.getContent());
        MessageCapsule saved = repository.save(MessageCapsule.create(messageId, conversationId, creatorId, safeTitle, unlockAt));
        searchIndexingService.indexCapsule(saved);
        return saved.getId();
    }

    @Transactional
    public void open(UUID capsuleId, UUID meId) {
        MessageCapsule capsule = repository.findById(capsuleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REQUEST));
        validateConversationAccess(capsule.getConversationId(), meId);
        if (capsule.getUnlockAt() != null && capsule.getUnlockAt().isAfter(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "아직 열 수 없는 타임 캡슐입니다.");
        }
        capsule.open();
    }

    @Transactional(readOnly = true)
    public MessageCapsuleListResponse listByConversation(UUID conversationId, UUID meId) {
        validateConversationAccess(conversationId, meId);
        return new MessageCapsuleListResponse(
                conversationId,
                repository.findByConversationIdOrderByUnlockAtDesc(conversationId).stream()
                        .map(c -> new MessageCapsuleListResponse.Item(
                                c.getId(),
                                c.getMessageId(),
                                c.getCreatorId(),
                                c.getTitle(),
                                c.getUnlockAt(),
                                c.getOpenedAt(),
                                c.getOpenedAt() != null
                        ))
                        .toList()
        );
    }

    @Transactional
    public void update(UUID capsuleId, UUID meId, String title, LocalDateTime unlockAt) {
        MessageCapsule capsule = repository.findById(capsuleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REQUEST));
        if (!capsule.getCreatorId().equals(meId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        if (unlockAt != null && !unlockAt.isAfter(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "열릴 시간은 현재보다 이후여야 합니다.");
        }
        capsule.update(sanitizeTitle(title, capsule.getTitle()), unlockAt);
        searchIndexingService.indexCapsule(capsule);
    }

    @Transactional
    public void delete(UUID capsuleId, UUID meId) {
        MessageCapsule capsule = repository.findById(capsuleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REQUEST));
        if (!capsule.getCreatorId().equals(meId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        repository.delete(capsule);
        searchIndexingService.remove("CAPSULES", capsule.getId());
    }

    private void validateConversationAccess(UUID conversationId, UUID meId) {
        boolean member = conversationMemberRepository.existsByConversationIdAndUserId(conversationId, meId);
        if (!member) {
            throw new BusinessException(ErrorCode.MESSAGE_FORBIDDEN);
        }
    }

    private String sanitizeTitle(String title, String fallback) {
        String candidate = title;
        if (candidate == null || candidate.isBlank()) {
            candidate = fallback;
        }
        if (candidate == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "캡슐 제목이 비어 있습니다.");
        }
        String normalized = candidate.replaceAll("\\s+", " ").trim();
        if (normalized.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "캡슐 제목이 비어 있습니다.");
        }
        return normalized.length() > 120 ? normalized.substring(0, 120) : normalized;
    }
}
