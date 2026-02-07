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

        // 대소문자 무시
        String k = keyword.toLowerCase();

        // prefix / contains 조건
        BooleanExpression prefix =
                u.handle.lower().startsWith(k)
                        .or(u.name.lower().startsWith(k));

        BooleanExpression contains =
                u.handle.lower().contains(k)
                        .or(u.name.lower().contains(k));

        // rank: prefix=0, contains=1 (그 외는 제외)
        NumberExpression<Integer> rankExpr = new CaseBuilder()
                .when(prefix).then(0)
                .when(contains).then(1)
                .otherwise(99);

        BooleanExpression searchCond = prefix.or(contains);

        // ASC 정렬용 커서 조건
        BooleanExpression cursorCond = null;
        if (cursor != null) {
            cursorCond =
                    rankExpr.gt(cursor.rank())
                            .or(rankExpr.eq(cursor.rank()).and(u.handle.gt(cursor.handle())))
                            .or(rankExpr.eq(cursor.rank()).and(u.handle.eq(cursor.handle())).and(u.id.gt(cursor.userId())));
        }

        // (선택) 나 자신 제외하고 싶으면 아래 조건 추가
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
                .orderBy(rankExpr.asc(), u.handle.asc(), u.id.asc())
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