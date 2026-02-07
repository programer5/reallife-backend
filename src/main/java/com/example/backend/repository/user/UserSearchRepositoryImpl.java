package com.example.backend.repository.user;

import com.example.backend.controller.user.dto.UserSearchResponse;
import com.example.backend.domain.user.QUser;
import com.querydsl.core.types.OrderSpecifier;
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
    public List<UserSearchResponse.Item> searchUsers(String q, UserSearchResponse.Cursor cursor, int limit) {
        QUser u = QUser.user;

        String query = q.trim();

        // rank: 0(정확) -> 1(handle prefix) -> 2(name prefix) -> 3(contains) -> 9(기타)
        NumberExpression<Integer> rankExpr = new CaseBuilder()
                .when(u.handle.equalsIgnoreCase(query)).then(0)
                .when(u.handle.startsWithIgnoreCase(query)).then(1)
                .when(u.name.startsWithIgnoreCase(query)).then(2)
                .when(u.handle.containsIgnoreCase(query).or(u.name.containsIgnoreCase(query))).then(3)
                .otherwise(9);

        BooleanExpression baseCond =
                u.deleted.isFalse()
                        .and(
                                u.handle.containsIgnoreCase(query)
                                        .or(u.name.containsIgnoreCase(query))
                        );

        BooleanExpression cursorCond = cursorCondition(u, rankExpr, cursor);

        // 정렬: rank ASC, handle ASC, id ASC (안정적인 커서용)
        OrderSpecifier<?>[] order = new OrderSpecifier[]{
                rankExpr.asc(),
                u.handle.asc(),
                u.id.asc()
        };

        return queryFactory
                .select(u.id, u.handle, u.name, u.followerCount, rankExpr)
                .from(u)
                .where(baseCond, cursorCond)
                .orderBy(order)
                .limit(limit)
                .fetch()
                .stream()
                .map(t -> new UserSearchResponse.Item(
                        t.get(u.id),
                        t.get(u.handle),
                        t.get(u.name),
                        t.get(u.followerCount) == null ? 0L : t.get(u.followerCount)
                ))
                .toList();
    }

    private BooleanExpression cursorCondition(QUser u, NumberExpression<Integer> rankExpr, UserSearchResponse.Cursor cursor) {
        if (cursor == null) return null;

        int r = cursor.rank();
        String h = cursor.handle();
        UUID id = cursor.userId();

        // ASC 정렬 기준으로 "다음 페이지" 조건
        // (rank > r) OR (rank == r AND handle > h) OR (rank == r AND handle == h AND id > id)
        return rankExpr.gt(r)
                .or(rankExpr.eq(r).and(u.handle.gt(h)))
                .or(rankExpr.eq(r).and(u.handle.eq(h)).and(u.id.gt(id)));
    }
}