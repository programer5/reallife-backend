package com.example.backend.domain.ops;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OpsAlertLogRepository extends JpaRepository<OpsAlertLog, Long> {

    List<OpsAlertLog> findTop20ByOrderByCreatedAtDesc();
}