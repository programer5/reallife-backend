package com.example.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

import java.time.LocalDateTime;

@MappedSuperclass
public abstract class BaseEntity {

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "update_at", nullable = false)
    private LocalDateTime updateAt;

    // ✅ 핵심: 기본값 false + insert 시 값 들어가게
    @Column(name = "deleted", nullable = false)
    private boolean deleted = false;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updateAt = now;
        // ✅ 혹시라도 null/초기화 누락 방지(primitive라 사실상 필요 없지만 안전)
        this.deleted = false;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updateAt = LocalDateTime.now();
    }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdateAt() { return updateAt; }
    public boolean isDeleted() { return deleted; }

    protected void markDeleted() {
        this.deleted = true;
    }
}
