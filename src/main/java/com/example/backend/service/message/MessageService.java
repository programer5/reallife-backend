package com.example.backend.service.message;

import com.example.backend.controller.message.dto.MessageSendResponse;
import com.example.backend.domain.message.Conversation;
import com.example.backend.domain.message.Message;
import com.example.backend.exception.BusinessException;
import com.example.backend.exception.ErrorCode;
import com.example.backend.repository.message.ConversationMemberRepository;
import com.example.backend.repository.message.ConversationRepository;
import com.example.backend.repository.message.MessageRepository;
import com.example.backend.service.file.LocalStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final ConversationRepository conversationRepository;
    private final ConversationMemberRepository memberRepository;
    private final MessageRepository messageRepository;
    private final LocalStorageService storageService;

    @Transactional
    public MessageSendResponse send(UUID meId, UUID conversationId, String content, List<MultipartFile> files) {

        Conversation conv = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MESSAGE_CONVERSATION_NOT_FOUND));

        if (!memberRepository.existsByConversationIdAndUserId(conv.getId(), meId)) {
            throw new BusinessException(ErrorCode.MESSAGE_FORBIDDEN);
        }

        boolean hasText = StringUtils.hasText(content);
        boolean hasFiles = files != null && files.stream().anyMatch(f -> f != null && !f.isEmpty());

        if (!hasText && !hasFiles) {
            throw new BusinessException(ErrorCode.MESSAGE_EMPTY);
        }

        // 파일 개수 제한
        if (hasFiles) {
            long count = files.stream().filter(f -> f != null && !f.isEmpty()).count();
            if (count > storageService.maxFilesPerMessage()) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST); // 원하면 별도 코드 추가 가능
            }
        }

        Message message = Message.create(conversationId, meId, hasText ? content : null);

        // 파일 저장 + attachment 추가
        if (hasFiles) {
            for (MultipartFile f : files) {
                if (f == null || f.isEmpty()) continue;
                var stored = storageService.store(conversationId, f);
                message.addAttachment(stored.fileKey(), stored.originalName(), stored.mimeType(), stored.sizeBytes());
            }
        }

        Message saved = messageRepository.save(message);

        var atts = saved.getAttachments().stream()
                .map(a -> new MessageSendResponse.AttachmentDto(
                        a.getId(),
                        a.getOriginalName(),
                        a.getMimeType(),
                        a.getSizeBytes(),
                        "/api/messages/attachments/" + a.getId() + "/download"
                ))
                .toList();

        return new MessageSendResponse(
                saved.getId(),
                saved.getConversationId(),
                saved.getSenderId(),
                saved.getContent(),
                saved.getCreatedAt(),
                atts
        );
    }
}