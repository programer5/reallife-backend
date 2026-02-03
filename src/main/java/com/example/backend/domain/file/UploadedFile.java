package com.example.backend.domain.file;

import com.example.backend.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

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

    @Column(name = "stored_filename", nullable = false, length = 255)
    private String storedFilename;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(nullable = false)
    private long size;

    @Column(nullable = false)
    private boolean deleted;

    private UploadedFile(UUID uploaderId, String originalFilename, String storedFilename, String contentType, long size) {
        this.uploaderId = uploaderId;
        this.originalFilename = originalFilename;
        this.storedFilename = storedFilename;
        this.contentType = contentType;
        this.size = size;
        this.deleted = false;
    }

    public static UploadedFile create(UUID uploaderId, String originalFilename, String storedFilename, String contentType, long size) {
        return new UploadedFile(uploaderId, originalFilename, storedFilename, contentType, size);
    }
}