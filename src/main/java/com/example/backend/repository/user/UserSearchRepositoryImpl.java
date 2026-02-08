package com.example.backend.repository.user;

import com.example.backend.controller.user.dto.UserSearchResponse;
import com.example.backend.domain.user.QUser;
import com.example.backend.repository.user.dto.UserSearchRow;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class UserSearchRepositoryImpl implements UserSearchRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<UserSearchRow> search(UUID meId, String q, UserSearchResponse.Cursor cursor, int limit) {
        QUser u = QUser.user;

        String keyword = (q == null) ? "" : q.trim();
        if (keyword.isEmpty()) return List.of();

        String k = keyword.toLowerCase();

        BooleanExpression prefix =
                u.handleLower.startsWith(k)
                        .or(u.nameLower.startsWith(k));

        BooleanExpression contains =
                u.handleLower.contains(k)
                        .or(u.nameLower.contains(k));

        NumberExpression<Integer> rankExpr = new CaseBuilder()
                .when(prefix).then(0)
                .when(contains).then(1)
                .otherwise(99);

        BooleanExpression searchCond = prefix.or(contains);

        BooleanExpression cursorCond = null;
        if (cursor != null) {
            cursorCond =
                    rankExpr.gt(cursor.rank())
                            .or(rankExpr.eq(cursor.rank()).and(u.followerCount.lt(cursor.followerCount())))
                            .or(rankExpr.eq(cursor.rank()).and(u.followerCount.eq(cursor.followerCount())).and(u.handleLower.gt(cursor.handle())))
                            .or(rankExpr.eq(cursor.rank()).and(u.followerCount.eq(cursor.followerCount())).and(u.handleLower.eq(cursor.handle())).and(u.id.gt(cursor.userId())));
        }

        BooleanExpression notMe = (meId == null) ? null : u.id.ne(meId);

        List<Tuple> rows = queryFactory
                .select(u.id, u.handle, u.name, u.followerCount, rankExpr)
                .from(u)
                .where(
                        u.deleted.isFalse(),
                        notMe,
                        searchCond,
                        cursorCond
                )
                // ✅ 인스타 느낌: rank(정확도) 우선, 그 다음 followerCount desc, 그 다음 handle asc, id asc
                .orderBy(rankExpr.asc(), u.followerCount.desc(), u.handleLower.asc(), u.id.asc())
                .limit(limit)
                .fetch();

        return rows.stream()
                .map(t -> new UserSearchRow(
                        t.get(u.id),
                        t.get(u.handle),
                        t.get(u.name),
                        t.get(u.followerCount),
                        t.get(rankExpr)
                ))
                .toList();
    }
}