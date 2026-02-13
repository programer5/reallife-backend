package com.example.backend.repository.message;

import com.example.backend.controller.message.dto.MessageListResponse;
import com.example.backend.domain.file.QUploadedFile;
import com.example.backend.domain.message.QMessage;
import com.example.backend.domain.message.QMessageAttachment;
import com.example.backend.domain.message.QMessageHidden;
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

    /**
     * 기존 시그니처(호환용): meId 없이 조회.
     * - 앞으로는 meId 버전을 사용하는 것을 권장
     */
    @Override
    public List<MessageListResponse.Item> fetchPage(
            UUID conversationId,
            LocalDateTime cursorCreatedAt,
            UUID cursorMessageId,
            int size
    ) {
        // meId가 없으면 "숨김 필터"를 적용할 수 없음.
        // 기존 호출부가 전부 meId 버전으로 바뀌면 이 메서드는 삭제해도 됨.
        return fetchPage(conversationId, null, cursorCreatedAt, cursorMessageId, size);
    }

    /**
     * ✅ 신규: meId 포함 조회
     * - deleted=false(모두 삭제 제외)
     * - NOT EXISTS message_hidden (나만 삭제(숨김) 제외)
     */
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

        // ✅ 숨김 필터: meId가 있을 때만 적용(호환용)
        BooleanExpression notHiddenCond = notHiddenForMe(meId, h, m);

        // 1) 메시지 id limit개(이미 최신순)
        List<UUID> messageIds = queryFactory
                .select(m.id)
                .from(m)
                .where(
                        m.conversationId.eq(conversationId),
                        m.deleted.isFalse(),      // ✅ 모두 삭제 제외
                        notHiddenCond,            // ✅ 나만 삭제(숨김) 제외
                        cursorCond
                )
                .orderBy(m.createdAt.desc(), m.id.desc())
                .limit(limit)
                .fetch();

        if (messageIds.isEmpty()) return List.of();

        // 2) 메시지 본문들 (IN 조회는 순서 보장 X)
        var messages = queryFactory
                .selectFrom(m)
                .where(m.id.in(messageIds))
                .fetch();

        // 3) 첨부 + 파일 메타를 한 번에(LEFT JOIN)
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

        // messageIds는 최신순 정렬, messages는 IN 조회라 순서 보장 X
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

        // DESC 페이징: (createdAt, id) < (cursorCreatedAt, cursorId)
        return m.createdAt.lt(cursorCreatedAt)
                .or(m.createdAt.eq(cursorCreatedAt).and(m.id.lt(cursorId)));
    }

    /**
     * ✅ NOT EXISTS message_hidden (user_id = meId, message_id = m.id)
     * - meId가 null이면(호환/테스트) 필터 미적용(null 반환)
     */
    private BooleanExpression notHiddenForMe(UUID meId, QMessageHidden h, QMessage m) {
        if (meId == null) return null;

        return JPAExpressions
                .selectOne()
                .from(h)
                .where(
                        h.userId.eq(meId),
                        h.messageId.eq(m.id)
                )
                .notExists();
    }
}