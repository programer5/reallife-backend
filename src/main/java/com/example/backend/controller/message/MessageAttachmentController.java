package com.example.backend.controller.message;

import com.example.backend.domain.file.UploadedFile;
import com.example.backend.domain.message.MessageAttachment;
import com.example.backend.exception.BusinessException;
import com.example.backend.exception.ErrorCode;
import com.example.backend.repository.file.UploadedFileRepository;
import com.example.backend.repository.message.ConversationMemberRepository;
import com.example.backend.repository.message.MessageAttachmentRepository;
import com.example.backend.repository.message.MessageRepository;
import com.example.backend.service.file.LocalStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/messages/attachments")
public class MessageAttachmentController {

    private final MessageAttachmentRepository attachmentRepository;
    private final MessageRepository messageRepository;
    private final ConversationMemberRepository memberRepository;
    private final UploadedFileRepository uploadedFileRepository;
    private final LocalStorageService storageService;

    @GetMapping("/{attachmentId}/download")
    public ResponseEntity<FileSystemResource> download(
            @PathVariable UUID attachmentId,
            Authentication authentication
    ) {
        UUID meId = UUID.fromString(authentication.getName());

        MessageAttachment att = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FILE_NOT_FOUND));

        var msg = messageRepository.findByIdAndDeletedFalse(att.getMessageId())
                .orElseThrow(() -> new BusinessException(ErrorCode.MESSAGE_NOT_FOUND));

        if (!memberRepository.existsByConversationIdAndUserId(msg.getConversationId(), meId)) {
            throw new BusinessException(ErrorCode.MESSAGE_FORBIDDEN);
        }

        UploadedFile file = uploadedFileRepository.findByIdAndDeletedFalse(att.getFileId())
                .orElseThrow(() -> new BusinessException(ErrorCode.FILE_NOT_FOUND));

        Path path = storageService.resolvePath(file.getFileKey());
        if (!path.toFile().exists()) throw new BusinessException(ErrorCode.FILE_NOT_FOUND);

        FileSystemResource resource = new FileSystemResource(path);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(file.getOriginalFilename()).build().toString())
                .body(resource);
    }
}
