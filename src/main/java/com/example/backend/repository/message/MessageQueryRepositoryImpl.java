package com.example.backend.repository.message;

import com.example.backend.controller.message.dto.MessageListResponse;
import com.example.backend.domain.file.QUploadedFile;
import com.example.backend.domain.message.QMessage;
import com.example.backend.domain.message.QMessageAttachment;
import com.example.backend.domain.message.QMessageHidden;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
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
        return fetchPage(conversationId, null, cursorCreatedAt, cursorMessageId, size);
    }

    @Override
    public List<MessageListResponse.Item> fetchPage(
            UUID conversationId,
            UUID meId,
            LocalDateTime cursorCreatedAt,
            UUID cursorMessageId,
            int limit
    ) {
        QMessage m = QMessage.message;
        QMessageAttachment a = QMessageAttachment.messageAttachment;
        QUploadedFile f = QUploadedFile.uploadedFile;
        QMessageHidden h = QMessageHidden.messageHidden;

        BooleanExpression cursorCond = cursor(cursorCreatedAt, cursorMessageId, m);

        // ✅ 숨김 필터: meId 있을 때만 적용
        BooleanExpression notHiddenCond = notHiddenForMe(meId, h, m);

        // 1) 메시지 id limit+1개
        List<UUID> messageIds = queryFactory
                .select(m.id)
                .from(m)
                .where(
                        m.conversationId.eq(conversationId),
                        // ✅ FIX: deletedFalse() 없음
                        m.deleted.isFalse(),
                        cursorCond,
                        notHiddenCond
                )
                .orderBy(m.createdAt.desc(), m.id.desc())
                .limit(limit + 1L)
                .fetch();

        if (messageIds.isEmpty()) return List.of();

        // 2) 메시지 본문
        List<com.example.backend.domain.message.Message> messages = queryFactory
                .selectFrom(m)
                .where(m.id.in(messageIds))
                .fetch();

        Map<UUID, com.example.backend.domain.message.Message> msgMap = new HashMap<>();
        for (var mm : messages) msgMap.put(mm.getId(), mm);

        // 3) 첨부 목록
        // ✅ FIX: QUploadedFile에 url 없음 -> fileId만으로 download URL 생성
        List<Tuple> tuples = queryFactory
                .select(
                        a.messageId,
                        f.id,
                        f.originalFilename,
                        f.contentType,
                        f.size,
                        a.sortOrder
                )
                .from(a)
                .join(f).on(f.id.eq(a.fileId))
                .where(a.messageId.in(messageIds))
                .orderBy(a.sortOrder.asc())
                .fetch();

        Map<UUID, List<MessageListResponse.Attachment>> attMap = new HashMap<>();

        for (Tuple t : tuples) {
            UUID messageId = t.get(a.messageId);
            UUID fileId = t.get(f.id);
            String originalFilename = t.get(f.originalFilename);
            String contentType = t.get(f.contentType);
            Long size = t.get(f.size);

            String downloadUrl = "/api/files/" + fileId + "/download";

            attMap.computeIfAbsent(messageId, k -> new ArrayList<>())
                    .add(new MessageListResponse.Attachment(
                            fileId,
                            downloadUrl,
                            originalFilename,
                            contentType,
                            size == null ? 0 : size
                    ));
        }

        // 4) 결과 조립(메시지 id 순서 유지)
        List<MessageListResponse.Item> result = new ArrayList<>();
        for (UUID id : messageIds) {
            var mm = msgMap.get(id);
            if (mm == null) continue;

            result.add(new MessageListResponse.Item(
                    mm.getId(),
                    mm.getSenderId(),
                    mm.getContent(),
                    mm.getCreatedAt(),
                    attMap.getOrDefault(mm.getId(), List.of()),
                    List.of() // pinCandidates는 MessageQueryService에서 채움
            ));
        }

        return result;
    }

    private BooleanExpression cursor(LocalDateTime cursorCreatedAt, UUID cursorId, QMessage m) {
        if (cursorCreatedAt == null || cursorId == null) return null;

        // DESC 페이징: (createdAt, id) < (cursorCreatedAt, cursorId)
        return m.createdAt.lt(cursorCreatedAt)
                .or(m.createdAt.eq(cursorCreatedAt).and(m.id.lt(cursorId)));
    }

    private BooleanExpression notHiddenForMe(UUID meId, QMessageHidden h, QMessage m) {
        if (meId == null) return null;

        return JPAExpressions.selectOne()
                .from(h)
                .where(
                        h.messageId.eq(m.id),
                        h.userId.eq(meId)
                )
                .notExists();
    }
}