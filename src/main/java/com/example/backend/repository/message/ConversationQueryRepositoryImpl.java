package com.example.backend.repository.message;

import com.example.backend.domain.message.ConversationType;
import com.example.backend.domain.message.QConversation;
import com.example.backend.domain.message.QConversationMember;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class ConversationQueryRepositoryImpl implements ConversationQueryRepository {

    private final JPAQueryFactory queryFactory;

    public ConversationQueryRepositoryImpl(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    @Override
    public Optional<UUID> findExistingDirectConversation(UUID meId, UUID targetUserId) {
        QConversation c = QConversation.conversation;
        QConversationMember m1 = new QConversationMember("m1");
        QConversationMember m2 = new QConversationMember("m2");

        UUID convId = queryFactory
                .select(c.id)
                .from(c)
                .join(m1).on(m1.conversationId.eq(c.id))
                .join(m2).on(m2.conversationId.eq(c.id))
                .where(
                        c.type.eq(ConversationType.DIRECT),
                        m1.userId.eq(meId),
                        m2.userId.eq(targetUserId)
                )
                .fetchFirst();

        return Optional.ofNullable(convId);
    }
}