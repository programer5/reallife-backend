package com.example.backend.repository.user;

import com.example.backend.controller.user.dto.UserSearchResponse;

import java.util.List;

public interface UserSearchRepository {
    List<UserSearchResponse.Item> searchUsers(
            String q,
            UserSearchResponse.Cursor cursor,
            int limit
    );
}