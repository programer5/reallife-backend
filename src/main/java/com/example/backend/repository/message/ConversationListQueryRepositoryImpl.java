package com.example.backend.repository.message;

import com.example.backend.domain.message.QConversation;
import com.example.backend.domain.message.QConversationMember;
import com.example.backend.domain.message.QMessage;
import com.example.backend.domain.user.QUser;
import com.example.backend.repository.message.dto.ConversationListRow;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.DateTimeExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Repository
public class ConversationListQueryRepositoryImpl implements ConversationListQueryRepository {

    private final JPAQueryFactory queryFactory;

    public ConversationListQueryRepositoryImpl(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    @Override
    public List<ConversationListRow> fetchConversationList(UUID meId, LocalDateTime cursorAt, UUID cursorConversationId, int sizePlusOne) {
        QConversationMember m = QConversationMember.conversationMember;
        QConversationMember peer = new QConversationMember("peer");
        QConversation c = QConversation.conversation;
        QUser u = QUser.user;

        DateTimeExpression<LocalDateTime> sortAt = Expressions.dateTimeTemplate(
                LocalDateTime.class,
                "coalesce({0}, {1})",
                c.lastMessageAt, c.createdAt
        );

        BooleanExpression base = m.userId.eq(meId)
                .and(m.deleted.isFalse())
                .and(c.deleted.isFalse());

        BooleanExpression cursorCond = null;
        if (cursorAt != null && cursorConversationId != null) {
            cursorCond = sortAt.lt(cursorAt)
                    .or(sortAt.eq(cursorAt).and(c.id.lt(cursorConversationId)));
        }

        BooleanExpression where = (cursorCond == null) ? base : base.and(cursorCond);

        return queryFactory
                .select(Projections.constructor(
                        ConversationListRow.class,
                        c.id,
                        peer.userId,
                        u.name,                              // ✅ nickname 대신 name
                        Expressions.nullExpression(String.class),
                        c.lastMessagePreview,
                        c.lastMessageAt,
                        sortAt
                ))
                .from(m)
                .join(c).on(c.id.eq(m.conversationId))
                .join(peer).on(peer.conversationId.eq(m.conversationId)
                        .and(peer.userId.ne(meId))
                        .and(peer.deleted.isFalse()))
                .join(u).on(u.id.eq(peer.userId))
                .where(where)
                .orderBy(sortAt.desc(), c.id.desc())
                .limit(sizePlusOne)
                .fetch();
    }

    @Override
    public Map<UUID, Long> fetchUnreadCounts(UUID meId, List<UUID> conversationIds) {
        if (conversationIds == null || conversationIds.isEmpty()) return Map.of();

        QConversationMember m = QConversationMember.conversationMember;
        QMessage msg = QMessage.message;

        LocalDateTime epoch = LocalDateTime.of(1970, 1, 1, 0, 0);

        List<Tuple> tuples = queryFactory
                .select(msg.conversationId, msg.count())
                .from(msg)
                .join(m).on(m.conversationId.eq(msg.conversationId).and(m.userId.eq(meId)))
                .where(
                        msg.conversationId.in(conversationIds),
                        msg.deleted.isFalse(),
                        msg.senderId.ne(meId),
                        msg.createdAt.gt(
                                Expressions.dateTimeTemplate(
                                        LocalDateTime.class,
                                        "coalesce({0}, {1})",
                                        m.lastReadAt,
                                        Expressions.constant(epoch)
                                )
                        )
                )
                .groupBy(msg.conversationId)
                .fetch();

        Map<UUID, Long> result = new HashMap<>();
        for (Tuple t : tuples) {
            result.put(t.get(msg.conversationId), t.get(msg.count()));
        }
        return result;
    }
}