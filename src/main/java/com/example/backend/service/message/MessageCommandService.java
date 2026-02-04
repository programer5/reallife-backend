package com.example.backend.service.message;

import com.example.backend.controller.message.dto.MessageSendRequest;
import com.example.backend.controller.message.dto.MessageSendResponse;
import com.example.backend.domain.file.UploadedFile;
import com.example.backend.domain.message.Message;
import com.example.backend.domain.message.MessageAttachment;
import com.example.backend.domain.message.event.MessageSentEvent;
import com.example.backend.exception.BusinessException;
import com.example.backend.exception.ErrorCode;
import com.example.backend.repository.file.UploadedFileRepository;
import com.example.backend.repository.message.ConversationParticipantRepository;
import com.example.backend.repository.message.MessageAttachmentRepository;
import com.example.backend.repository.message.MessageRepository;
import com.example.backend.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageCommandService {

    private final MessageRepository messageRepository;
    private final MessageAttachmentRepository attachmentRepository;
    private final UploadedFileRepository uploadedFileRepository;
    private final ConversationParticipantRepository participantRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher; // ✅ 추가

    @Transactional
    public MessageSendResponse send(UUID meId, UUID conversationId, MessageSendRequest req) {

        if (!participantRepository.existsByConversationIdAndUserId(conversationId, meId)) {
            throw new BusinessException(ErrorCode.MESSAGE_FORBIDDEN);
        }

        userRepository.findById(meId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        String content = (req.content() == null) ? null : req.content().trim();
        List<UUID> attachmentIds = (req.attachmentIds() == null) ? List.of() : req.attachmentIds();

        if ((content == null || content.isBlank()) && attachmentIds.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        Message saved = messageRepository.save(Message.text(conversationId, meId, content));

        // 첨부 저장 + 응답 구성
        List<MessageSendResponse.FileItem> files = new ArrayList<>();

        for (int i = 0; i < attachmentIds.size(); i++) {
            UUID fileId = attachmentIds.get(i);

            UploadedFile file = uploadedFileRepository.findByIdAndDeletedFalse(fileId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.FILE_NOT_FOUND));

            attachmentRepository.save(MessageAttachment.create(saved.getId(), fileId, i));

            files.add(new MessageSendResponse.FileItem(
                    file.getId(),
                    "/api/files/" + file.getId() + "/download",
                    file.getOriginalFilename(),
                    file.getContentType(),
                    file.getSize()
            ));
        }

        // ✅ 이벤트 발행 (추가)
        eventPublisher.publishEvent(new MessageSentEvent(
                saved.getId(),
                saved.getConversationId(),
                saved.getSenderId(),
                saved.getContent(),
                saved.getCreatedAt()
        ));

        log.info("📨 MessageSentEvent published | messageId={}", saved.getId());

        return new MessageSendResponse(
                saved.getId(),
                saved.getConversationId(),
                saved.getSenderId(),
                saved.getContent(),
                files,
                saved.getCreatedAt()
        );
    }
}