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

        // 나 자신 제외(선택)
        BooleanExpression notMe = (meId == null) ? null : u.id.ne(meId);

        // ✅ 정렬: rank ASC, followerCount DESC, handle ASC, id ASC
        // ✅ 커서 조건은 "정렬 기준 그대로" 다음 페이지를 찾도록 작성해야 함
        BooleanExpression cursorCond = null;
        if (cursor != null) {
            cursorCond =
                    // rank가 더 큰 그룹(뒤쪽)
                    rankExpr.gt(cursor.rank())

                            // rank 동일 + followerCount가 더 작은 것(뒤쪽)  <-- DESC라서 작은 값이 뒤로 감
                            .or(rankExpr.eq(cursor.rank()).and(u.followerCount.lt(cursor.followerCount())))

                            // rank 동일 + followerCount 동일 + handle 더 큰 것(뒤쪽)  <-- ASC
                            .or(rankExpr.eq(cursor.rank())
                                    .and(u.followerCount.eq(cursor.followerCount()))
                                    .and(u.handle.gt(cursor.handle())))

                            // rank 동일 + followerCount 동일 + handle 동일 + id 더 큰 것(뒤쪽) <-- ASC
                            .or(rankExpr.eq(cursor.rank())
                                    .and(u.followerCount.eq(cursor.followerCount()))
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