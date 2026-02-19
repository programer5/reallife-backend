package com.example.backend.repository.post;

import com.example.backend.domain.post.Post;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PostRepository extends JpaRepository<Post, UUID>, PostQueryRepository {

    Optional<Post> findByIdAndAuthorIdAndDeletedFalse(UUID id, UUID authorId);

    /**
     * ✅ 피드에서 이미지까지 한 번에 가져오기 (N+1 방지)
     */
    @Query("select distinct p from Post p left join fetch p.images img where p.id in :ids and p.deleted = false")
    List<Post> findAllWithImagesByIdIn(@Param("ids") List<UUID> ids);
}
