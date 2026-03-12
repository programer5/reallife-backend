package com.example.backend.domain.file;

import com.example.backend.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

import java.util.Locale;
import java.util.UUID;

import static lombok.AccessLevel.PROTECTED;

@Entity
@Getter
@NoArgsConstructor(access = PROTECTED)
@Table(
        name = "uploaded_files",
        indexes = {
                @Index(name = "idx_uploaded_uploader", columnList = "uploader_id")
        }
)
public class UploadedFile extends BaseEntity {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "uploader_id", nullable = false)
    private UUID uploaderId;

    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;

    @Column(name = "file_key", nullable = false, length = 500)
    private String fileKey;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(nullable = false)
    private long size;

    // ✅ 썸네일
    @Column(name = "thumbnail_file_key", length = 500)
    private String thumbnailFileKey;

    @Column(name = "thumbnail_content_type", length = 100)
    private String thumbnailContentType;

    @Column(name = "thumbnail_size")
    private Long thumbnailSize;

    @Enumerated(EnumType.STRING)
    @Column(name = "file_type", nullable = false, length = 20)
    private UploadedFileType fileType;

    private UploadedFile(UUID uploaderId, String originalFilename, String fileKey, String contentType, long size) {
        this.uploaderId = uploaderId;
        this.originalFilename = originalFilename;
        this.fileKey = fileKey;
        this.contentType = contentType;
        this.size = size;
        this.fileType = resolveFileType(contentType);
    }

    public static UploadedFile create(UUID uploaderId, String originalFilename, String fileKey, String contentType, long size) {
        return new UploadedFile(uploaderId, originalFilename, fileKey, contentType, size);
    }

    public void attachThumbnail(String thumbnailFileKey, String thumbnailContentType, long thumbnailSize) {
        this.thumbnailFileKey = thumbnailFileKey;
        this.thumbnailContentType = thumbnailContentType;
        this.thumbnailSize = thumbnailSize;
    }

    public boolean hasThumbnail() {
        return thumbnailFileKey != null && !thumbnailFileKey.isBlank();
    }

    public void delete() {
        markDeleted(); // ✅ BaseEntity.deleted 사용
    }

    private UploadedFileType resolveFileType(String contentType) {
        String ct = contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);
        if (ct.startsWith("image/")) return UploadedFileType.IMAGE;
        if (ct.startsWith("video/")) return UploadedFileType.VIDEO;
        return UploadedFileType.FILE;
    }
}