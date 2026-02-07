package com.example.backend.service.user;

import com.example.backend.controller.user.dto.UserSearchResponse;
import com.example.backend.repository.user.UserSearchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserSearchService {

    private final UserSearchRepository userSearchRepository;

    public UserSearchResponse search(String q, String cursor, Integer size) {
        if (q == null || q.isBlank()) {
            // q가 비면 빈 결과로 처리(클라 UX 위해 400 대신)
            return new UserSearchResponse(List.of(), null, false);
        }

        int pageSize = normalizeSize(size);

        UserSearchResponse.Cursor decoded = UserSearchResponse.Cursor.decode(cursor);

        // size+1로 hasNext 판단
        List<UserSearchResponse.Item> fetched = userSearchRepository.searchUsers(
                q,
                decoded,
                pageSize + 1
        );

        boolean hasNext = fetched.size() > pageSize;
        List<UserSearchResponse.Item> page = hasNext ? fetched.subList(0, pageSize) : fetched;

        String nextCursor = null;
        if (hasNext && !page.isEmpty()) {
            UserSearchResponse.Item last = page.get(page.size() - 1);

            // last의 rank를 다시 계산해야 커서가 완전해짐
            // → repository에서 rank도 같이 반환하는 방식으로 개선 가능.
            // MVP에서는 rank 재계산(동일 로직)을 여기서 수행.
            int rank = calcRank(q, last.handle(), last.name());
            nextCursor = UserSearchResponse.Cursor.encode(rank, last.handle(), last.userId());
        }

        return new UserSearchResponse(page, nextCursor, hasNext);
    }

    private int normalizeSize(Integer size) {
        int v = (size == null) ? 20 : size;
        if (v < 1) v = 1;
        if (v > 50) v = 50;
        return v;
    }

    private int calcRank(String q, String handle, String name) {
        String query = q.trim();
        if (handle != null && handle.equalsIgnoreCase(query)) return 0;
        if (handle != null && handle.toLowerCase().startsWith(query.toLowerCase())) return 1;
        if (name != null && name.startsWith(query)) return 2;
        if ((handle != null && handle.toLowerCase().contains(query.toLowerCase())) ||
                (name != null && name.contains(query))) return 3;
        return 9;
    }
}