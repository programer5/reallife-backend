
package com.example.backend.controller.message;

import com.example.backend.service.message.MessageCapsuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/capsules")
public class MessageCapsuleController {

    private final MessageCapsuleService service;

    @PostMapping
    public UUID create(@RequestParam UUID messageId,
                       @RequestParam UUID conversationId,
                       @RequestParam String title,
                       @RequestParam String unlockAt,
                       @RequestParam UUID userId){
        return service.create(messageId,conversationId,userId,title,LocalDateTime.parse(unlockAt));
    }

    @PostMapping("/{capsuleId}/open")
    public void open(@PathVariable UUID capsuleId){
        service.open(capsuleId);
    }
}
