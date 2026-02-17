package com.example.backend.repository.file;

import com.example.backend.domain.file.UploadedFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UploadedFileRepository extends JpaRepository<UploadedFile, UUID> {

    Optional<UploadedFile> findByIdAndDeletedFalse(UUID id);

    // ✅ 고아 파일(GC) 조회: 프로필/게시글 어디에도 참조되지 않는 파일
    @Query("""
    select f
    from UploadedFile f
    where f.deleted = false
      and f.createdAt < :threshold
      and not exists (select 1 from User u where u.profileImageFileId = f.id)
      and not exists (select 1 from PostImage pi where pi.file.id = f.id)
    """)
    List<UploadedFile> findOrphanFiles(@Param("threshold") LocalDateTime threshold);
}