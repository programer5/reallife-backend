package com.example.backend.repository.user;

import com.example.backend.controller.user.dto.UserSearchResponse;
import com.example.backend.domain.user.QUser;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.core.types.dsl.StringExpression;
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
        StringExpression handleLower = u.handle.lower();
        StringExpression nameLower = u.name.lower();

        // ✅ rank 기준
        // -1: handle 정확 일치
        //  0: prefix (handle/name)
        //  1: contains (handle/name)
        BooleanExpression exact = handleLower.eq(k);
        BooleanExpression prefix = handleLower.startsWith(k).or(nameLower.startsWith(k));
        BooleanExpression contains = handleLower.contains(k).or(nameLower.contains(k));

        NumberExpression<Integer> rankExpr = new CaseBuilder()
                .when(exact).then(-1)
                .when(prefix).then(0)
                .when(contains).then(1)
                .otherwise(99);

        BooleanExpression searchCond = exact.or(prefix).or(contains);

        // (선택) 나 자신 제외
        BooleanExpression notMe = (meId == null) ? null : u.id.ne(meId);

        // ✅ 커서 조건 (orderBy와 100% 동일한 튜플 비교)
        // order: rank ASC, followerCount DESC, handle ASC, id ASC
        BooleanExpression cursorCond = null;
        if (cursor != null) {
            cursorCond = rankExpr.gt(cursor.rank())
                    .or(rankExpr.eq(cursor.rank()).and(u.followerCount.lt(cursor.followerCount())))
                    .or(rankExpr.eq(cursor.rank()).and(u.followerCount.eq(cursor.followerCount()))
                            .and(u.handle.gt(cursor.handle())))
                    .or(rankExpr.eq(cursor.rank()).and(u.followerCount.eq(cursor.followerCount()))
                            .and(u.handle.eq(cursor.handle()))
                            .and(u.id.gt(cursor.userId())));
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
                        u.handle.asc(),
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