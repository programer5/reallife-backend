package com.example.backend.repository.error;

import com.example.backend.domain.error.ErrorLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ErrorLogRepository extends JpaRepository<ErrorLog, UUID> {

    List<ErrorLog> findTop10ByOrderByCreatedAtDesc();
}