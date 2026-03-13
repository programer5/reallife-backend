
package com.example.backend.domain.post;

import com.example.backend.domain.file.UploadedFile;
import com.example.backend.domain.file.UploadedFileType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

import static lombok.AccessLevel.PROTECTED;

@Entity
@Getter
@NoArgsConstructor(access = PROTECTED)
@Table(name = "post_images")
public class PostImage {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @Column(nullable = false, length = 1000)
    private String imageUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id")
    private UploadedFile file;

    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", nullable = false, length = 20)
    private PostMediaType mediaType;

    @Column(name = "thumbnail_url", length = 1000)
    private String thumbnailUrl;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @Column(nullable = false)
    private int sortOrder;

    private PostImage(Post post, String imageUrl, UploadedFile file, PostMediaType mediaType, String thumbnailUrl, String contentType, int sortOrder) {
        this.post = post;
        this.imageUrl = imageUrl;
        this.file = file;
        this.mediaType = mediaType;
        this.thumbnailUrl = thumbnailUrl;
        this.contentType = contentType;
        this.sortOrder = sortOrder;
    }

    public static PostImage create(Post post, String imageUrl, int sortOrder) {
        return new PostImage(post, imageUrl, null, PostMediaType.IMAGE, imageUrl, "image/*", sortOrder);
    }

    public static PostImage create(Post post, UploadedFile file, String imageUrl, int sortOrder) {
        PostMediaType type = (file != null && file.getFileType() == UploadedFileType.VIDEO) ? PostMediaType.VIDEO : PostMediaType.IMAGE;
        String thumb = imageUrl;
        if (file != null && file.hasThumbnail()) {
            thumb = "/api/files/" + file.getId() + "/thumbnail";
        }
        String contentType = file != null ? file.getContentType() : null;
        return new PostImage(post, imageUrl, file, type, thumb, contentType, sortOrder);
    }
}
