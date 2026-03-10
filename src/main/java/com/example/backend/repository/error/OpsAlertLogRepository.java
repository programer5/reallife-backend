package com.example.backend.repository.error;

import com.example.backend.domain.error.OpsAlertLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OpsAlertLogRepository extends JpaRepository<OpsAlertLog, UUID> {

    List<OpsAlertLog> findTop20ByOrderByCreatedAtDesc();
}