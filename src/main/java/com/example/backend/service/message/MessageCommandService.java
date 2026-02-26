package com.example.backend.service.message;

import com.example.backend.controller.message.dto.MessageSendRequest;
import com.example.backend.controller.message.dto.MessageSendResponse;
import com.example.backend.domain.file.UploadedFile;
import com.example.backend.domain.message.Conversation;
import com.example.backend.domain.message.Message;
import com.example.backend.domain.message.MessageAttachment;
import com.example.backend.domain.message.event.MessageSentEvent;
import com.example.backend.exception.BusinessException;
import com.example.backend.exception.ErrorCode;
import com.example.backend.repository.file.UploadedFileRepository;
import com.example.backend.repository.message.ConversationMemberRepository;
import com.example.backend.repository.message.ConversationRepository;
import com.example.backend.repository.message.MessageAttachmentRepository;
import com.example.backend.repository.message.MessageRepository;
import com.example.backend.repository.user.UserRepository;
import com.example.backend.service.pin.ConversationPinService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class MessageCommandService {

    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final ConversationMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final UploadedFileRepository uploadedFileRepository;
    private final MessageAttachmentRepository attachmentRepository;
    private final ConversationLockService lockService;
    private final ApplicationEventPublisher eventPublisher;

    // ✅ NEW: 핀 서비스
    private final ConversationPinService pinService;

    public MessageSendResponse send(UUID meId, UUID conversationId, MessageSendRequest req, String unlockToken) {

        if (!memberRepository.existsByConversationIdAndUserId(conversationId, meId)) {
            throw new BusinessException(ErrorCode.MESSAGE_FORBIDDEN);
        }

        // ✅ DM Lock: 잠금된 대화는 unlock token 없으면 차단
        lockService.ensureUnlocked(conversationId, meId, unlockToken);

        userRepository.findById(meId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        String content = (req.content() == null) ? null : req.content().trim();
        List<UUID> attachmentIds = (req.attachmentIds() == null) ? List.of() : req.attachmentIds();

        if ((content == null || content.isBlank()) && attachmentIds.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        Message saved = messageRepository.save(Message.text(conversationId, meId, req.content()));

        // ✅ Conversation 조회
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONVERSATION_NOT_FOUND));

        // ✅ preview 업데이트
        String preview = req.content() == null ? "" : req.content();
        preview = preview.length() > 200 ? preview.substring(0, 200) : preview;
        conversation.updateLastMessage(saved.getId(), saved.getCreatedAt(), preview);

        // ✅ NEW: 핀 감지/생성 (메시지 저장은 절대 깨지지 않게)
        try {
            pinService.tryDetectAndCreateFromMessage(meId, conversationId, saved.getContent());
        } catch (Exception e) {
            log.warn("pin detection failed (ignored) | conversationId={} messageId={}", conversationId, saved.getId(), e);
        }

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

        // 이벤트 발행
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