package com.example.backend.repository.post;

import com.example.backend.domain.post.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PostRepository extends JpaRepository<Post, UUID>, PostQueryRepository {

    Optional<Post> findByIdAndAuthorIdAndDeletedFalse(UUID id, UUID authorId);

    /**
     * ✅ Feed에서 imageUrls까지 내려주기 위한 fetch join
     * - "팔로잉 피드"는 pageSize가 작으므로(<=50) IN 조회 + fetch join이 현실적
     * - distinct로 중복 row 제거
     */
    @Query("""
            select distinct p
            from Post p
            left join fetch p.images imgs
            where p.id in :ids
            """)
    List<Post> findAllWithImagesByIdIn(@Param("ids") List<UUID> ids);
}
