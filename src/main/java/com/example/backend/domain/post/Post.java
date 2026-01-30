package com.example.backend.domain.post;

import com.example.backend.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static lombok.AccessLevel.PROTECTED;

@Entity
@Getter
@NoArgsConstructor(access = PROTECTED)
@Table(name = "posts")
public class Post extends BaseEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private UUID authorId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PostVisibility visibility;

    @Column(nullable = false)
    private long likeCount;

    @Column(nullable = false)
    private long commentCount;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private final List<PostImage> images = new ArrayList<>();

    @Column(nullable = false)
    private boolean deleted;

    private Post(UUID authorId, String content, PostVisibility visibility) {
        this.authorId = authorId;
        this.content = content;
        this.visibility = visibility;
        this.likeCount = 0;
        this.commentCount = 0;
        this.deleted = false;
    }

    public static Post create(UUID authorId, String content, PostVisibility visibility) {
        return new Post(authorId, content, visibility);
    }

    public void addImage(String imageUrl, int sortOrder) {
        this.images.add(PostImage.create(this, imageUrl, sortOrder));
    }
}
