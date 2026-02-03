package com.example.backend.repository.message;

import com.example.backend.controller.message.dto.MessageListResponse;
import com.example.backend.domain.file.QUploadedFile;
import com.example.backend.domain.message.QMessage;
import com.example.backend.domain.message.QMessageAttachment;
import com.querydsl.core.Tuple;
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

        // 1) 메시지 size+1개 (hasNext 계산용)
        List<com.example.backend.domain.message.Message> messages = queryFactory
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

        // 2) 첨부 + 파일메타를 한 번에 조회
        //    - a.id: attachmentId (다운로드 URL 만들 때 필요)
        //    - a.messageId: 어떤 메시지에 달렸는지
        //    - a.sortOrder: 정렬
        //    - f: 파일 메타
        List<Tuple> rows = queryFactory
                .select(
                        a.id,          // attachmentId
                        a.messageId,
                        a.sortOrder,
                        f.id,          // fileId
                        f.originalFilename,
                        f.contentType,
                        f.size
                )
                .from(a)
                .join(f).on(
                        f.id.eq(a.fileId),
                        f.deleted.isFalse()
                )
                .where(a.messageId.in(messageIds))
                .orderBy(a.messageId.asc(), a.sortOrder.asc(), a.createdAt.asc())
                .fetch();

        // messageId -> attachments
        Map<UUID, List<MessageListResponse.Attachment>> attMap = new HashMap<>();
        for (Tuple t : rows) {
            UUID attachmentId = t.get(a.id);
            UUID messageId = t.get(a.messageId);

            UUID fileId = t.get(f.id);
            String originalName = t.get(f.originalFilename);
            String mimeType = t.get(f.contentType);
            Long sizeBytes = t.get(f.size);

            attMap.computeIfAbsent(messageId, k -> new ArrayList<>())
                    .add(new MessageListResponse.Attachment(
                            fileId,
                            "/api/messages/attachments/" + attachmentId + "/download",
                            originalName,
                            mimeType,
                            sizeBytes == null ? 0L : sizeBytes
                    ));
        }

        // 3) MessageListResponse.Item 만들기
        List<MessageListResponse.Item> result = new ArrayList<>();
        for (var msg : messages) {
            result.add(new MessageListResponse.Item(
                    msg.getId(),
                    msg.getSenderId(),
                    msg.getContent(),
                    msg.getCreatedAt(),
                    attMap.getOrDefault(msg.getId(), List.of())
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