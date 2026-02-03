package com.example.backend.repository.message;

import com.example.backend.domain.message.Message;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, UUID> {

    // 최신 메시지부터 cursor paging: (createdAt desc, id desc)
    List<Message> findTop21ByConversationIdOrderByCreatedAtDescIdDesc(UUID conversationId); // size=20 고정 MVP용

    // 커서 이후(더 과거): createdAt < cursor OR (createdAt == cursor AND id < cursorId)
    // -> 이건 JPA 메소드명으로 가면 지옥이라 QueryDSL로 가는 게 맞는데,
    // MVP는 "첫 페이지"만 먼저 하거나, QueryDSL로 구현하자.
}