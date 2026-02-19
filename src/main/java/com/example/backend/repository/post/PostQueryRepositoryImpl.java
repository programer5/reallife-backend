package com.example.backend.repository.post;

import com.example.backend.domain.follow.QFollow;
import com.example.backend.domain.post.PostVisibility;
import com.example.backend.domain.post.QPost;
import com.example.backend.domain.user.QUser;
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
    public List<UUID> findFollowingFeedIdsFirstPage(UUID meId, int size) {
        return queryFeedIds(meId, null, null, size);
    }

    @Override
    public List<UUID> findFollowingFeedIdsNextPage(UUID meId, LocalDateTime cursorCreatedAt, UUID cursorId, int size) {
        return queryFeedIds(meId, cursorCreatedAt, cursorId, size);
    }

    private List<UUID> queryFeedIds(UUID meId, LocalDateTime cursorCreatedAt, UUID cursorId, int size) {
        QPost p = QPost.post;
        QFollow f = QFollow.follow;
        QUser u = QUser.user;

        BooleanExpression cursorCond = cursorCond(cursorCreatedAt, cursorId, p);

        BooleanExpression authorCond =
                f.followerId.eq(meId).and(f.followingId.eq(p.authorId))
                        .or(p.authorId.eq(meId));

        BooleanExpression visibilityCond =
                p.visibility.eq(PostVisibility.ALL)
                        .or(p.authorId.eq(meId));

        return queryFactory
                .select(p.id)
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
                .limit(size)
                .fetch();
    }

    private BooleanExpression cursorCond(LocalDateTime cursorCreatedAt, UUID cursorId, QPost p) {
        if (cursorCreatedAt == null || cursorId == null) return null;
        return p.createdAt.lt(cursorCreatedAt)
                .or(p.createdAt.eq(cursorCreatedAt).and(p.id.lt(cursorId)));
    }
}
