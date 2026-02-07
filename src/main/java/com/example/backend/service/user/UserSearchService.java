package com.example.backend.service.user;

import com.example.backend.controller.user.dto.UserSearchResponse;
import com.example.backend.repository.user.UserSearchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserSearchService {

    private final UserSearchRepository userSearchRepository;

    public UserSearchResponse search(UUID meId, String q, String cursorRaw, Integer size) {
        int pageSize = normalizeSize(size);
        UserSearchResponse.Cursor cursor = UserSearchResponse.Cursor.decode(cursorRaw);

        List<UserSearchResponse.Item> fetched = userSearchRepository.search(
                meId,
                q,
                cursor,
                pageSize + 1
        );

        boolean hasNext = fetched.size() > pageSize;
        List<UserSearchResponse.Item> page = hasNext ? fetched.subList(0, pageSize) : fetched;

        String nextCursor = null;
        if (hasNext && !page.isEmpty()) {
            var last = page.get(page.size() - 1);
            nextCursor = UserSearchResponse.Cursor.encode(last.rank(), last.handle(), last.userId());
        }

        return new UserSearchResponse(page, nextCursor, hasNext);
    }

    private int normalizeSize(Integer size) {
        int v = (size == null) ? 20 : size;
        if (v < 1) v = 1;
        if (v > 50) v = 50;
        return v;
    }
}