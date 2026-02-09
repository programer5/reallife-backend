package com.example.backend.service.user;

import com.example.backend.controller.user.dto.UserSearchResponse;
import com.example.backend.repository.follow.FollowRepository;
import com.example.backend.repository.user.UserSearchRepository;
import com.example.backend.repository.user.dto.UserSearchRow;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserSearchService {

    private final UserSearchRepository userSearchRepository;
    private final FollowRepository followRepository;

    public UserSearchResponse search(UUID meId, String q, String cursorRaw, Integer size) {
        int pageSize = normalizeSize(size);
        UserSearchResponse.Cursor cursor = UserSearchResponse.Cursor.decode(cursorRaw);

        List<UserSearchRow> fetched = userSearchRepository.search(
                meId,
                q,
                cursor,
                pageSize + 1
        );

        boolean hasNext = fetched.size() > pageSize;
        List<UserSearchRow> page = hasNext ? fetched.subList(0, pageSize) : fetched;

        // ✅ page에 포함된 userId들에 대해 내가 팔로우 중인지 한번에 조회
        List<UUID> targetIds = page.stream().map(UserSearchRow::userId).toList();
        HashSet<UUID> followingSet = new HashSet<>();
        if (!targetIds.isEmpty()) {
            followingSet.addAll(followRepository.findFollowingIds(meId, targetIds));
        }

        List<UserSearchResponse.Item> items = page.stream()
                .map(r -> new UserSearchResponse.Item(
                        r.userId(),
                        r.handle(),
                        r.name(),
                        r.followerCount(),
                        r.rank(),
                        followingSet.contains(r.userId())
                ))
                .toList();

        String nextCursor = null;
        if (hasNext && !page.isEmpty()) {
            var last = page.get(page.size() - 1);
            nextCursor = UserSearchResponse.Cursor.encode(last.rank(), last.followerCount(), last.handle().toLowerCase(), last.userId());
        }

        return new UserSearchResponse(items, nextCursor, hasNext);
    }

    private int normalizeSize(Integer size) {
        int v = (size == null) ? 20 : size;
        if (v < 1) v = 1;
        if (v > 50) v = 50;
        return v;
    }
}