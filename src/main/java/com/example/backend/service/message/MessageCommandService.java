package com.example.backend.service.message;

import com.example.backend.common.PublicUrlBuilder;
import com.example.backend.controller.message.dto.MessageSendRequest;
import com.example.backend.controller.message.dto.MessageSendResponse;
import com.example.backend.controller.message.dto.MessageUpdateRequest;
import com.example.backend.controller.message.dto.MessageUpdateResponse;
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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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

    // ✅ NEW: 파일 다운로드 URL 생성용
    private final PublicUrlBuilder urlBuilder;

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

        // ✅ NEW: 핀 후보 감지(Confirm 모드) - DB 저장 X
        List<com.example.backend.controller.message.dto.PinCandidateResponse> pinCandidates;
        try {
            pinCandidates = pinService.detectCandidates(saved.getId(), saved.getContent());
        } catch (Exception e) {
            log.warn("pin candidate detection failed (ignored) | conversationId={} messageId={}", conversationId, saved.getId(), e);
            pinCandidates = List.of();
        }

        // 첨부 저장 + 응답 구성
        List<MessageSendResponse.FileItem> files = new ArrayList<>();

        for (int i = 0; i < attachmentIds.size(); i++) {
            UUID fileId = attachmentIds.get(i);

            UploadedFile file = uploadedFileRepository.findByIdAndDeletedFalse(fileId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.FILE_NOT_FOUND));

            attachmentRepository.save(MessageAttachment.create(saved.getId(), fileId, i));

            // ✅ UploadedFile에는 getUrl()이 없다 -> 다운로드 URL을 만들어서 내려준다
            String downloadUrl = urlBuilder.absolute("/api/files/" + file.getId() + "/download");

            files.add(new MessageSendResponse.FileItem(
                    file.getId(),
                    downloadUrl,
                    file.getOriginalFilename(),
                    file.getContentType(),
                    file.getSize()
            ));
        }

        // ✅ SSE broadcast(메시지)
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
                pinCandidates,
                saved.getCreatedAt()
        );
    }

    @Transactional
    public MessageUpdateResponse update(UUID meId, UUID conversationId, UUID messageId, MessageUpdateRequest req) {

        // 1) 대화 멤버 권한
        if (!memberRepository.existsByConversationIdAndUserId(conversationId, meId)) {
            throw new BusinessException(ErrorCode.MESSAGE_FORBIDDEN);
        }

        // 2) 메시지 존재 + 삭제 아닌 것
        Message message = messageRepository.findByIdAndDeletedFalse(messageId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MESSAGE_NOT_FOUND));

        // 3) 대화방 매칭
        if (!message.getConversationId().equals(conversationId)) {
            throw new BusinessException(ErrorCode.MESSAGE_NOT_FOUND);
        }

        // 4) 작성자만 수정 가능
        if (!message.getSenderId().equals(meId)) {
            throw new BusinessException(ErrorCode.MESSAGE_FORBIDDEN);
        }

        // 5) 내용 검증
        String content = req.content() == null ? null : req.content().trim();
        if (content == null || content.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        if (content.length() > 5000) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        // 6) 업데이트
        var now = java.time.LocalDateTime.now();
        message.updateContent(content, now);

        // 7) lastMessagePreview라면 preview도 갱신
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONVERSATION_NOT_FOUND));

        if (conversation.getLastMessageId() != null && conversation.getLastMessageId().equals(messageId)) {
            String preview = content.length() > 200 ? content.substring(0, 200) : content;
            // lastMessageAt은 “생성 시각” 유지 (바꾸지 않음)
            conversation.updateLastMessage(conversation.getLastMessageId(), conversation.getLastMessageAt(), preview);
        }

        // 8) SSE 이벤트 발행
        eventPublisher.publishEvent(new com.example.backend.domain.message.event.MessageUpdatedEvent(
                message.getId(),
                message.getConversationId(),
                meId,
                message.getContent(),
                message.getEditedAt()
        ));

        return new MessageUpdateResponse(
                message.getId(),
                message.getConversationId(),
                message.getContent(),
                message.getEditedAt()
        );
    }
}