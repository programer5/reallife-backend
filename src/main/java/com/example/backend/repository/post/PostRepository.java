package com.example.backend.repository.post;

import com.example.backend.domain.post.Post;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PostRepository extends JpaRepository<Post, UUID>, PostQueryRepository {

    Optional<Post> findByIdAndAuthorIdAndDeletedFalse(UUID id, UUID authorId);

}
