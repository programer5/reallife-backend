package com.example.backend.service.message.listener;

import com.example.backend.domain.message.event.MessageSentEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MessageEventLoggingListener {

    @EventListener
    public void onMessageSent(MessageSentEvent event) {
        log.info(
                "📨 MessageSentEvent | msgId={} convId={} senderId={} contentLength={}",
                event.messageId(),
                event.conversationId(),
                event.senderId(),
                event.content() == null ? 0 : event.content().length()
        );
    }
}