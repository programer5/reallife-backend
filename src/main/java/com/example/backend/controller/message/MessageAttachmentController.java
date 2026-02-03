package com.example.backend.controller.message;

import com.example.backend.domain.message.MessageAttachment;
import com.example.backend.exception.BusinessException;
import com.example.backend.exception.ErrorCode;
import com.example.backend.repository.message.ConversationMemberRepository;
import com.example.backend.repository.message.MessageAttachmentRepository;
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
    private final ConversationMemberRepository memberRepository;
    private final LocalStorageService storageService;

    @GetMapping("/{attachmentId}/download")
    public ResponseEntity<FileSystemResource> download(
            @PathVariable UUID attachmentId,
            Authentication authentication
    ) {
        UUID meId = UUID.fromString(authentication.getName());

        MessageAttachment att = attachmentRepository.findByIdWithMessage(attachmentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FILE_NOT_FOUND));

        UUID convId = att.getMessage().getConversationId();
        if (!memberRepository.existsByConversationIdAndUserId(convId, meId)) {
            throw new BusinessException(ErrorCode.MESSAGE_FORBIDDEN);
        }

        Path path = storageService.resolvePath(att.getFileKey());
        if (!path.toFile().exists()) throw new BusinessException(ErrorCode.FILE_NOT_FOUND);

        FileSystemResource resource = new FileSystemResource(path);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(att.getMimeType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(att.getOriginalName()).build().toString())
                .body(resource);
    }
}