package com.example.backend.domain.post;

import com.example.backend.domain.BaseEntity;
import com.example.backend.domain.file.UploadedFile;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

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
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
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


    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "place_name", length = 120)
    private String placeName;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private final List<PostImage> images = new ArrayList<>();

    private Post(UUID authorId, String content, PostVisibility visibility) {
        this.authorId = authorId;
        this.content = content;
        this.visibility = visibility;
        this.likeCount = 0;
        this.commentCount = 0;
    }

    public static Post create(UUID authorId, String content, PostVisibility visibility) {
        return new Post(authorId, content, visibility);
    }

    /** ✅ 기존 방식(유지): url 문자열 저장 */
    public void addImage(String imageUrl, int sortOrder) {
        this.images.add(PostImage.create(this, imageUrl, sortOrder));
    }

    /** ✅ 새 방식(추가): UploadedFile 기반 + url도 같이 저장(프론트 편의) */
    public void addImage(UploadedFile file, int sortOrder) {
        addMedia(file, sortOrder);
    }

    public void addMedia(UploadedFile file, int sortOrder) {
        String url = "/api/files/" + file.getId() + "/download";
        this.images.add(PostImage.create(this, file, url, sortOrder));
    }

    public void update(String content, PostVisibility visibility) {
        if (content != null && !content.isBlank()) {
            this.content = content;
        }
        if (visibility != null) {
            this.visibility = visibility;
        }
    }

    public void updateLocation(Double latitude, Double longitude, String placeName) {
        if (latitude != null && longitude != null && isValidCoordinate(latitude, longitude)) {
            this.latitude = latitude;
            this.longitude = longitude;
        }
        if (placeName != null) {
            String value = placeName.trim();
            this.placeName = value.isEmpty() ? null : value.substring(0, Math.min(value.length(), 120));
        }
    }

    private boolean isValidCoordinate(Double latitude, Double longitude) {
        return latitude >= -90 && latitude <= 90 && longitude >= -180 && longitude <= 180;
    }

    public void delete() {
        markDeleted();
    }

    public void increaseLikeCount() {
        this.likeCount++;
    }

    public void decreaseLikeCount() {
        if (this.likeCount > 0) this.likeCount--;
    }

    public void increaseCommentCount() {
        this.commentCount++;
    }

    public void decreaseCommentCount() {
        if (this.commentCount > 0) this.commentCount--;
    }

}
