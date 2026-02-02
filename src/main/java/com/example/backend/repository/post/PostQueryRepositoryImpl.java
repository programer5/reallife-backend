package com.example.backend.repository.post;

import com.example.backend.controller.post.dto.FeedResponse;
import com.example.backend.domain.follow.QFollow;
import com.example.backend.domain.post.PostVisibility;
import com.example.backend.domain.post.QPost;
import com.example.backend.domain.user.QUser;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public class PostQueryRepositoryImpl implements PostQueryRepository {

    private final JPAQueryFactory queryFactory;

    public PostQueryRepositoryImpl(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    @Override
    public List<FeedResponse.FeedItem> findFollowingFeedFirstPage(UUID meId, int size) {
        return queryFeed(meId, null, null, size);
    }

    @Override
    public List<FeedResponse.FeedItem> findFollowingFeedNextPage(
            UUID meId,
            LocalDateTime cursorCreatedAt,
            UUID cursorId,
            int size
    ) {
        return queryFeed(meId, cursorCreatedAt, cursorId, size);
    }

    private List<FeedResponse.FeedItem> queryFeed(
            UUID meId,
            LocalDateTime cursorCreatedAt,
            UUID cursorId,
            int size
    ) {
        QPost p = QPost.post;
        QFollow f = QFollow.follow;
        QUser u = QUser.user;

        BooleanExpression cursorCond = cursorCond(cursorCreatedAt, cursorId, p);

        // ✅ 팔로우한 사람 글 + 내 글
        BooleanExpression authorCond =
                f.followerId.eq(meId).and(f.followingId.eq(p.authorId))
                        .or(p.authorId.eq(meId));

        // ✅ 남의 글은 ALL만, 내 글은 전부
        BooleanExpression visibilityCond =
                p.visibility.eq(PostVisibility.ALL)
                        .or(p.authorId.eq(meId));

        return queryFactory
                .select(Projections.constructor(
                        FeedResponse.FeedItem.class,
                        p.id,                 // ✅ UUID 그대로
                        p.authorId,           // ✅ UUID 그대로
                        u.handle,
                        u.name,
                        p.content,
                        com.querydsl.core.types.dsl.Expressions.constant(List.of()), // ✅ 일단 빈 리스트
                        p.visibility.stringValue(),
                        p.createdAt
                ))
                .from(p)
                .join(u).on(u.id.eq(p.authorId))
                .leftJoin(f).on(f.followerId.eq(meId).and(f.followingId.eq(p.authorId)))
                .where(
                        p.deleted.isFalse(),
                        authorCond,
                        visibilityCond,
                        cursorCond
                )
                .orderBy(p.createdAt.desc(), p.id.desc())
                .limit(size + 1L) // ✅ hasNext 판별용
                .fetch();
    }

    private BooleanExpression cursorCond(LocalDateTime cursorCreatedAt, UUID cursorId, QPost p) {
        if (cursorCreatedAt == null || cursorId == null) return null;
        return p.createdAt.lt(cursorCreatedAt)
                .or(p.createdAt.eq(cursorCreatedAt).and(p.id.lt(cursorId)));
    }
}