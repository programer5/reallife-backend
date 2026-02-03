package com.example.backend.repository.file;

import com.example.backend.domain.file.UploadedFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UploadedFileRepository extends JpaRepository<UploadedFile, UUID> {
    Optional<UploadedFile> findByIdAndDeletedFalse(UUID id);
}