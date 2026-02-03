package com.example.backend.controller.message;

import com.example.backend.domain.message.Message;
import com.example.backend.domain.message.MessageAttachment;
import com.example.backend.exception.BusinessException;
import com.example.backend.exception.ErrorCode;
import com.example.backend.repository.message.ConversationParticipantRepository;
import com.example.backend.repository.message.MessageAttachmentRepository;
import com.example.backend.repository.message.MessageRepository;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/messages/attachments")
public class MessageAttachmentController {

    private final MessageAttachmentRepository attachmentRepository;
    private final MessageRepository messageRepository;
    private final ConversationParticipantRepository participantRepository;

    public MessageAttachmentController(
            MessageAttachmentRepository attachmentRepository,
            MessageRepository messageRepository,
            ConversationParticipantRepository participantRepository
    ) {
        this.attachmentRepository = attachmentRepository;
        this.messageRepository = messageRepository;
        this.participantRepository = participantRepository;
    }

    @GetMapping("/{attachmentId}/download")
    public ResponseEntity<Void> download(
            @PathVariable UUID attachmentId,
            Authentication authentication
    ) {
        UUID meId = UUID.fromString(authentication.getName());

        MessageAttachment att = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FILE_NOT_FOUND));

        Message msg = messageRepository.findById(att.getMessageId())
                .orElseThrow(() -> new BusinessException(ErrorCode.MESSAGE_NOT_FOUND));

        if (!participantRepository.existsByConversationIdAndUserId(msg.getConversationId(), meId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        // ✅ 실제 다운로드는 files 컨트롤러로 위임
        URI redirect = URI.create("/api/files/" + att.getFileId());
        return ResponseEntity.status(302)
                .header(HttpHeaders.LOCATION, redirect.toString())
                .build();
    }
}