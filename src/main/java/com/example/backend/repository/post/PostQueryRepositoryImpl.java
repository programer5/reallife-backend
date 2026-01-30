package com.example.backend.repository.post;

import com.example.backend.domain.post.Post;
import com.example.backend.domain.post.PostVisibility;
import com.example.backend.domain.post.QPost;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class PostQueryRepositoryImpl implements PostQueryRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<Post> findFeedFirstPage(PostVisibility visibility, int size) {
        QPost p = QPost.post;

        return queryFactory
                .selectFrom(p)
                .where(
                        p.deleted.isFalse(),
                        p.visibility.eq(visibility)
                )
                .orderBy(p.createdAt.desc(), p.id.desc())
                .limit(size)
                .fetch();
    }

    @Override
    public List<Post> findFeedNextPage(PostVisibility visibility, LocalDateTime cursorCreatedAt, UUID cursorId, int size) {
        QPost p = QPost.post;

        return queryFactory
                .selectFrom(p)
                .where(
                        p.deleted.isFalse(),
                        p.visibility.eq(visibility),
                        p.createdAt.lt(cursorCreatedAt)
                                .or(p.createdAt.eq(cursorCreatedAt).and(p.id.lt(cursorId)))
                )
                .orderBy(p.createdAt.desc(), p.id.desc())
                .limit(size)
                .fetch();
    }

    @Override
    public List<Post> findFollowingFeedFirstPage(Collection<UUID> authorIds, int size) {
        QPost p = QPost.post;

        return queryFactory.selectFrom(p)
                .where(
                        p.deleted.isFalse(),
                        p.visibility.eq(PostVisibility.ALL),
                        p.authorId.in(authorIds)
                )
                .orderBy(p.createdAt.desc(), p.id.desc())
                .limit(size)
                .fetch();
    }

    @Override
    public List<Post> findFollowingFeedNextPage(Collection<UUID> authorIds, LocalDateTime cursorCreatedAt, UUID cursorId, int size) {
        QPost p = QPost.post;

        return queryFactory.selectFrom(p)
                .where(
                        p.deleted.isFalse(),
                        p.visibility.eq(PostVisibility.ALL),
                        p.authorId.in(authorIds),
                        p.createdAt.lt(cursorCreatedAt)
                                .or(p.createdAt.eq(cursorCreatedAt).and(p.id.lt(cursorId)))
                )
                .orderBy(p.createdAt.desc(), p.id.desc())
                .limit(size)
                .fetch();
    }

}
