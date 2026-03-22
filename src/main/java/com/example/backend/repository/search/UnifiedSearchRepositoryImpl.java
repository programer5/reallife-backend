package com.example.backend.repository.search;

import com.example.backend.domain.message.QConversationMember;
import com.example.backend.domain.message.QMessage;
import com.example.backend.domain.message.QMessageCapsule;
import com.example.backend.domain.pin.QConversationPin;
import com.example.backend.domain.post.PostVisibility;
import com.example.backend.domain.post.QPost;
import com.example.backend.repository.search.dto.SearchRow;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.*;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class UnifiedSearchRepositoryImpl implements UnifiedSearchRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<SearchRow> searchMessages(UUID meId, String query, UUID conversationId, int limit) {
        QMessage m = QMessage.message;
        QConversationMember cm = QConversationMember.conversationMember;
        NumberExpression<Integer> relevance = scoreMessage(m.content, query);

        return queryFactory
                .select(Projections.constructor(
                        SearchRow.class,
                        Expressions.constant("MESSAGES"),
                        m.id,
                        coalesce(m.content, "메시지"),
                        snippet(m.content, 180),
                        coalesce(m.content, ""),
                        m.createdAt,
                        m.conversationId,
                        Expressions.constant("메시지"),
                        Expressions.stringTemplate("'대화방 메시지'"),
                        relevance
                ))
                .from(m)
                .join(cm).on(cm.conversationId.eq(m.conversationId), cm.userId.eq(meId), cm.deleted.isFalse())
                .where(
                        m.deleted.isFalse(),
                        containsIgnoreCase(m.content, query),
                        conversationEquals(m.conversationId, conversationId)
                )
                .orderBy(relevance.desc(), m.createdAt.desc(), m.id.desc())
                .limit(limit)
                .fetch();
    }

    @Override
    public List<SearchRow> searchPins(UUID meId, String query, UUID conversationId, int limit) {
        QConversationPin p = QConversationPin.conversationPin;
        QConversationMember cm = QConversationMember.conversationMember;

        BooleanExpression textCond = containsIgnoreCase(p.title, query)
                .or(containsIgnoreCase(p.placeText, query));
        NumberExpression<Integer> relevance = scorePin(p.title, p.placeText, query);

        return queryFactory
                .select(Projections.constructor(
                        SearchRow.class,
                        Expressions.constant("ACTIONS"),
                        p.id,
                        coalesce(p.title, "액션"),
                        Expressions.stringTemplate("concat(coalesce({0}, ''), case when {1} is null then '' else concat(' · ', {1}) end)", p.title, p.placeText),
                        coalesce(p.title, "액션"),
                        p.createdAt,
                        p.conversationId,
                        p.type.stringValue(),
                        p.status.stringValue(),
                        relevance
                ))
                .from(p)
                .join(cm).on(cm.conversationId.eq(p.conversationId), cm.userId.eq(meId), cm.deleted.isFalse())
                .where(
                        p.deleted.isFalse(),
                        textCond,
                        conversationEquals(p.conversationId, conversationId)
                )
                .orderBy(relevance.desc(), p.createdAt.desc(), p.id.desc())
                .limit(limit)
                .fetch();
    }

    @Override
    public List<SearchRow> searchCapsules(UUID meId, String query, UUID conversationId, int limit) {
        QMessageCapsule c = QMessageCapsule.messageCapsule;
        QConversationMember cm = QConversationMember.conversationMember;
        NumberExpression<Integer> relevance = scoreTitle(c.title, query, 172, 146, 112);

        return queryFactory
                .select(Projections.constructor(
                        SearchRow.class,
                        Expressions.constant("CAPSULES"),
                        c.id,
                        coalesce(c.title, "캡슐"),
                        coalesce(c.title, ""),
                        coalesce(c.title, ""),
                        c.unlockAt,
                        c.conversationId,
                        Expressions.constant("캡슐"),
                        c.unlockAt.stringValue(),
                        relevance
                ))
                .from(c)
                .join(cm).on(cm.conversationId.eq(c.conversationId), cm.userId.eq(meId), cm.deleted.isFalse())
                .where(
                        containsIgnoreCase(c.title, query),
                        conversationEquals(c.conversationId, conversationId)
                )
                .orderBy(relevance.desc(), c.unlockAt.desc(), c.id.desc())
                .limit(limit)
                .fetch();
    }

    @Override
    public List<SearchRow> searchPosts(UUID meId, String query, int limit) {
        QPost p = QPost.post;
        NumberExpression<Integer> relevance = scoreTitle(p.content, query, 128, 102, 74);
        return queryFactory
                .select(Projections.constructor(
                        SearchRow.class,
                        Expressions.constant("POSTS"),
                        p.id,
                        coalesce(p.content, "게시글"),
                        snippet(p.content, 180),
                        coalesce(p.content, ""),
                        p.createdAt,
                        Expressions.nullExpression(UUID.class),
                        Expressions.constant("피드"),
                        p.visibility.stringValue(),
                        relevance
                ))
                .from(p)
                .where(
                        p.deleted.isFalse(),
                        p.visibility.eq(PostVisibility.ALL),
                        containsIgnoreCase(p.content, query)
                )
                .orderBy(relevance.desc(), p.createdAt.desc(), p.id.desc())
                .limit(limit)
                .fetch();
    }

    private BooleanExpression containsIgnoreCase(StringPath path, String value) {
        if (value == null || value.isBlank()) return null;
        return path.containsIgnoreCase(value.trim());
    }

    private Expression<String> coalesce(Expression<String> path, String fallback) {
        return Expressions.stringTemplate("coalesce({0}, {1})", path, Expressions.constant(fallback));
    }

    private Expression<String> snippet(StringPath path, int maxLen) {
        return Expressions.stringTemplate(
                "case when length(coalesce({0}, '')) > {1} then concat(substring(coalesce({0}, ''), 1, {1}), '…') else coalesce({0}, '') end",
                path,
                Expressions.constant(maxLen)
        );
    }

    private NumberExpression<Integer> scoreMessage(StringPath content, String query) {
        return scoreTitle(content, query, 148, 122, 86);
    }

    private NumberExpression<Integer> scorePin(StringPath title, StringPath placeText, String query) {
        String q = normalizeQuery(query);
        if (q == null) return Expressions.asNumber(0);
        return Expressions.numberTemplate(Integer.class,
                "case " +
                        "when lower(coalesce({0}, '')) = {2} then {3} " +
                        "when lower(coalesce({0}, '')) like concat({2}, '%') then {4} " +
                        "when lower(coalesce({0}, '')) like concat('%', {2}, '%') then {5} " +
                        "when lower(coalesce({1}, '')) like concat('%', {2}, '%') then {6} " +
                        "else 0 end",
                title,
                placeText,
                Expressions.constant(q),
                Expressions.constant(168),
                Expressions.constant(142),
                Expressions.constant(108),
                Expressions.constant(84)
        );
    }

    private NumberExpression<Integer> scoreTitle(StringPath path, String query, int exact, int prefix, int contains) {
        String q = normalizeQuery(query);
        if (q == null) return Expressions.asNumber(0);
        return Expressions.numberTemplate(Integer.class,
                "case " +
                        "when lower(coalesce({0}, '')) = {1} then {2} " +
                        "when lower(coalesce({0}, '')) like concat({1}, '%') then {3} " +
                        "when lower(coalesce({0}, '')) like concat('%', {1}, '%') then {4} " +
                        "else 0 end",
                path,
                Expressions.constant(q),
                Expressions.constant(exact),
                Expressions.constant(prefix),
                Expressions.constant(contains)
        );
    }

    private String normalizeQuery(String query) {
        if (query == null || query.isBlank()) return null;
        return query.trim().toLowerCase();
    }

    private BooleanExpression conversationEquals(SimpleExpression<UUID> path, UUID conversationId) {
        if (conversationId == null) return null;
        return path.eq(conversationId);
    }
}
