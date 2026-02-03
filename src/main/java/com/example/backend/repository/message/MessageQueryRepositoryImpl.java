package com.example.backend.repository.message;

import com.example.backend.controller.message.dto.MessageListResponse;
import com.example.backend.domain.file.QUploadedFile;
import com.example.backend.domain.message.QMessage;
import com.example.backend.domain.message.QMessageAttachment;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.*;

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
        QUploadedFile f = QUploadedFile.uploadedFile;

        BooleanExpression cursorCond = cursor(cursorCreatedAt, cursorMessageId, m);

        // 1) 메시지 size+1
        List<UUID> messageIds = queryFactory
                .select(m.id)
                .from(m)
                .where(
                        m.conversationId.eq(conversationId),
                        m.deleted.isFalse(),
                        cursorCond
                )
                .orderBy(m.createdAt.desc(), m.id.desc())
                .limit(size + 1L)
                .fetch();

        if (messageIds.isEmpty()) return List.of();

        // 2) 메시지 본문들
        var messages = queryFactory
                .selectFrom(m)
                .where(m.id.in(messageIds))
                .fetch();

        // 3) 첨부 + 파일 메타를 한 번에(LEFT JOIN)
        //    a.messageId == m.id 형태로 조인
        var rows = queryFactory
                .select(
                        a.messageId,
                        a.fileId,
                        a.sortOrder,
                        f.originalFilename,
                        f.contentType,
                        f.size
                )
                .from(a)
                .leftJoin(f).on(f.id.eq(a.fileId).and(f.deleted.isFalse()))
                .where(a.messageId.in(messageIds))
                .orderBy(a.messageId.asc(), a.sortOrder.asc())
                .fetch();

        Map<UUID, List<MessageListResponse.Attachment>> attMap = new HashMap<>();
        for (var t : rows) {
            UUID msgId = t.get(a.messageId);
            UUID fileId = t.get(a.fileId);

            if (fileId == null) continue;

            String originalName = t.get(f.originalFilename);
            String mimeType = t.get(f.contentType);
            Long sizeBytes = t.get(f.size);

            attMap.computeIfAbsent(msgId, k -> new ArrayList<>())
                    .add(new MessageListResponse.Attachment(
                            fileId,
                            "/api/files/" + fileId + "/download",
                            originalName,
                            mimeType,
                            sizeBytes == null ? 0L : sizeBytes
                    ));
        }

        // messageIds는 최신순 정렬이 이미 되어있는데, messages는 IN 조회라 순서 보장 X
        // 따라서 id->Message 매핑 후 messageIds 순서대로 조립
        Map<UUID, com.example.backend.domain.message.Message> msgMap = new HashMap<>();
        for (var mm : messages) msgMap.put(mm.getId(), mm);

        List<MessageListResponse.Item> result = new ArrayList<>();
        for (UUID id : messageIds) {
            var mm = msgMap.get(id);
            if (mm == null) continue;

            result.add(new MessageListResponse.Item(
                    mm.getId(),
                    mm.getSenderId(),
                    mm.getContent(),
                    mm.getCreatedAt(),
                    attMap.getOrDefault(mm.getId(), List.of())
            ));
        }

        return result;
    }

    private BooleanExpression cursor(LocalDateTime cursorCreatedAt, UUID cursorId, QMessage m) {
        if (cursorCreatedAt == null || cursorId == null) return null;

        return m.createdAt.lt(cursorCreatedAt)
                .or(m.createdAt.eq(cursorCreatedAt).and(m.id.lt(cursorId)));
    }
}