package com.example.backend.repository.user;

import com.example.backend.controller.user.dto.UserSearchResponse;
import com.example.backend.domain.user.QUser;
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
    public List<UserSearchResponse.Item> search(UUID meId, String q, UserSearchResponse.Cursor cursor, int limit) {
        QUser u = QUser.user;

        String keyword = (q == null) ? "" : q.trim();
        if (keyword.isEmpty()) return List.of();

        String k = keyword.toLowerCase();

        // ✅ lower 컬럼 기반 prefix / contains
        BooleanExpression prefix =
                u.handleLower.startsWith(k)
                        .or(u.nameLower.startsWith(k));

        BooleanExpression contains =
                u.handleLower.contains(k)
                        .or(u.nameLower.contains(k));

        // rank: prefix=0, contains=1
        NumberExpression<Integer> rankExpr = new CaseBuilder()
                .when(prefix).then(0)
                .when(contains).then(1)
                .otherwise(99);

        BooleanExpression searchCond = prefix.or(contains);

        // 나 자신 제외
        BooleanExpression notMe = (meId == null) ? null : u.id.ne(meId);

        // ✅ 정렬: rank ASC, followerCount DESC, handleLower ASC, id ASC
        // ✅ 커서 조건: (정렬 기준의 "다음"을 정확히 표현)
        BooleanExpression cursorCond = null;
        if (cursor != null) {
            cursorCond =
                    rankExpr.gt(cursor.rank())
                            .or(rankExpr.eq(cursor.rank()).and(u.followerCount.lt(cursor.followerCount())))
                            .or(rankExpr.eq(cursor.rank()).and(u.followerCount.eq(cursor.followerCount())).and(u.handleLower.gt(cursor.handle())))
                            .or(rankExpr.eq(cursor.rank()).and(u.followerCount.eq(cursor.followerCount())).and(u.handleLower.eq(cursor.handle())).and(u.id.gt(cursor.userId())));
        }

        List<Tuple> rows = queryFactory
                .select(u.id, u.handle, u.name, u.followerCount, rankExpr)
                .from(u)
                .where(
                        u.deleted.isFalse(),
                        notMe,
                        searchCond,
                        cursorCond
                )
                .orderBy(
                        rankExpr.asc(),
                        u.followerCount.desc(),
                        u.handleLower.asc(),
                        u.id.asc()
                )
                .limit(limit)
                .fetch();

        return rows.stream()
                .map(t -> new UserSearchResponse.Item(
                        t.get(u.id),
                        t.get(u.handle),
                        t.get(u.name),
                        t.get(u.followerCount),
                        t.get(rankExpr)
                ))
                .toList();
    }
}