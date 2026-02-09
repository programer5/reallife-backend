package com.example.backend.repository.user;

import com.example.backend.controller.user.dto.UserSearchResponse;
import com.example.backend.repository.user.dto.UserSearchRow;

import java.util.List;
import java.util.UUID;

public interface UserSearchRepository {

    List<UserSearchRow> search(
            UUID meId,
            String q,
            UserSearchResponse.Cursor cursor,
            int limit // pageSize + 1
    );
}