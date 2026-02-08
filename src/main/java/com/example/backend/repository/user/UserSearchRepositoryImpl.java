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

        // ✅ 이제 lower() 계산하지 말고, 컬럼(handleLower/nameLower)로 검색
        BooleanExpression prefix =
                u.handleLower.startsWith(k)
                        .or(u.nameLower.startsWith(k));

        BooleanExpression contains =
                u.handleLower.contains(k)
                        .or(u.nameLower.contains(k));

        // rank: prefix=0, contains=1 (그 외는 제외)
        NumberExpression<Integer> rankExpr = new CaseBuilder()
                .when(prefix).then(0)
                .when(contains).then(1)
                .otherwise(99);

        BooleanExpression searchCond = prefix.or(contains);

        // (선택) 나 자신 제외
        BooleanExpression notMe = (meId == null) ? null : u.id.ne(meId);

        // ✅ 정렬 기준(인스타 느낌)
        // rank ASC, followerCount DESC, handleLower ASC, id ASC
        BooleanExpression cursorCond = buildCursorCond(u, rankExpr, cursor);

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

    private BooleanExpression buildCursorCond(QUser u,
                                              NumberExpression<Integer> rankExpr,
                                              UserSearchResponse.Cursor cursor) {
        if (cursor == null) return null;

        int cRank = cursor.rank();
        long cFollower = cursor.followerCount();
        String cHandleLower = (cursor.handle() == null) ? "" : cursor.handle().toLowerCase();
        UUID cUserId = cursor.userId();

        // 다음 페이지 조건(정렬 기준과 "완전히 동일"해야 함)
        // (rank > cRank)
        // OR (rank == cRank AND followerCount < cFollower)   // DESC 이므로 "작은 쪽"이 다음
        // OR (rank == cRank AND followerCount == cFollower AND handleLower > cHandleLower)
        // OR (rank == cRank AND followerCount == cFollower AND handleLower == cHandleLower AND id > cUserId)
        return rankExpr.gt(cRank)
                .or(rankExpr.eq(cRank).and(u.followerCount.lt(cFollower)))
                .or(rankExpr.eq(cRank).and(u.followerCount.eq(cFollower)).and(u.handleLower.gt(cHandleLower)))
                .or(rankExpr.eq(cRank).and(u.followerCount.eq(cFollower)).and(u.handleLower.eq(cHandleLower)).and(u.id.gt(cUserId)));
    }
}