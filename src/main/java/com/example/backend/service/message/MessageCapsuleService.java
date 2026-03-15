package com.example.backend.service.message;

import com.example.backend.controller.message.dto.MessageCapsuleListResponse;
import com.example.backend.domain.message.MessageCapsule;
import com.example.backend.exception.BusinessException;
import com.example.backend.exception.ErrorCode;
import com.example.backend.repository.message.MessageCapsuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MessageCapsuleService {

    private final MessageCapsuleRepository repository;

    @Transactional
    public UUID create(UUID messageId, UUID conversationId, UUID creatorId, String title, LocalDateTime unlockAt) {
        MessageCapsule saved = repository.save(MessageCapsule.create(messageId, conversationId, creatorId, title, unlockAt));
        return saved.getId();
    }

    @Transactional
    public void open(UUID capsuleId) {
        repository.findById(capsuleId).ifPresent(MessageCapsule::open);
    }

    @Transactional
    public void delete(UUID capsuleId, UUID meId) {
        MessageCapsule capsule = repository.findById(capsuleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MESSAGE_NOT_FOUND));

        if (!capsule.getCreatorId().equals(meId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        repository.delete(capsule);
    }

    @Transactional(readOnly = true)
    public MessageCapsuleListResponse listByConversation(UUID conversationId) {
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
}
