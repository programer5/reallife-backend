package com.example.backend.domain.post;

import com.example.backend.domain.file.UploadedFile;
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

    /**
     * ✅ 기존 방식 호환용(유지)
     * - 기존 데이터/기존 API가 imageUrl을 사용 중이므로 당장 제거하지 않음
     */
    @Column(nullable = false, length = 1000)
    private String imageUrl;

    /**
     * ✅ 정석 방식(추가)
     * - 앞으로는 UploadedFile을 참조하도록 확장
     * - 기존 데이터는 file_id가 NULL이어도 동작해야 하므로 nullable
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id")
    private UploadedFile file;

    @Column(nullable = false)
    private int sortOrder;

    private PostImage(Post post, String imageUrl, UploadedFile file, int sortOrder) {
        this.post = post;
        this.imageUrl = imageUrl;
        this.file = file;
        this.sortOrder = sortOrder;
    }

    /** ✅ 기존 호환 생성자(유지) */
    public static PostImage create(Post post, String imageUrl, int sortOrder) {
        return new PostImage(post, imageUrl, null, sortOrder);
    }

    /** ✅ 새 정석 생성자(추가): file 기반 + url도 함께 저장 */
    public static PostImage create(Post post, UploadedFile file, String imageUrl, int sortOrder) {
        return new PostImage(post, imageUrl, file, sortOrder);
    }
}