package com.example.backend.repository.message;

import com.example.backend.controller.message.dto.MessageListResponse;
import com.example.backend.domain.message.QMessage;
import com.example.backend.domain.message.QMessageAttachment;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Repository
public class MessageQueryRepositoryImpl implements MessageQueryRepository {

    private final JPAQueryFactory queryFactory;

    public MessageQueryRepositoryImpl(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    @Override
    public List<MessageListResponse.Item> fetchPage(
            UUID conversationId,
            LocalDateTime cursorCreatedAt,
            UUID cursorMessageId,
            int size
    ) {
        QMessage m = QMessage.message;
        QMessageAttachment a = QMessageAttachment.messageAttachment;

        BooleanExpression cursorCond = cursor(cursorCreatedAt, cursorMessageId, m);

        // 1) 메시지 먼저 size+1개 가져오기
        var messages = queryFactory
                .selectFrom(m)
                .where(
                        m.conversationId.eq(conversationId),
                        m.deleted.isFalse(),
                        cursorCond
                )
                .orderBy(m.createdAt.desc(), m.id.desc())
                .limit(size + 1L)
                .fetch();

        if (messages.isEmpty()) return List.of();

        List<UUID> messageIds = messages.stream().map(x -> x.getId()).toList();

        // 2) 첨부는 IN으로 가져오기
        var attachments = queryFactory
                .selectFrom(a)
                .where(a.message.id.in(messageIds))
                .orderBy(a.createdAt.asc())
                .fetch();

        Map<UUID, List<MessageListResponse.Attachment>> attMap = attachments.stream()
                .collect(Collectors.groupingBy(
                        x -> x.getMessage().getId(),
                        Collectors.mapping(
                                x -> new MessageListResponse.Attachment(
                                        x.getId(),
                                        x.getOriginalName(),
                                        x.getMimeType(),
                                        x.getSizeBytes(),
                                        "/api/messages/attachments/" + x.getId() + "/download"
                                ),
                                Collectors.toList()
                        )
                ));

        return messages.stream()
                .map(x -> new MessageListResponse.Item(
                        x.getId(),
                        x.getSenderId(),
                        x.getContent(),
                        x.getCreatedAt(),
                        attMap.getOrDefault(x.getId(), List.of())
                ))
                .toList();
    }

    private BooleanExpression cursor(LocalDateTime cursorCreatedAt, UUID cursorId, QMessage m) {
        if (cursorCreatedAt == null || cursorId == null) return null;

        return m.createdAt.lt(cursorCreatedAt)
                .or(m.createdAt.eq(cursorCreatedAt).and(m.id.lt(cursorId)));
    }
}