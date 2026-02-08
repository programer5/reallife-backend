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

    @Column(name = "deleted", nullable = false)
    private boolean deleted = false;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updateAt = now;
        this.deleted = false;

        beforePersist(); // ✅ 자식 훅
    }

    @PreUpdate
    protected void onUpdate() {
        this.updateAt = LocalDateTime.now();

        beforeUpdate(); // ✅ 자식 훅
    }

    /**
     * 자식 엔티티가 PrePersist 시점에 추가 작업이 필요하면 override
     */
    protected void beforePersist() {
        // default no-op
    }

    /**
     * 자식 엔티티가 PreUpdate 시점에 추가 작업이 필요하면 override
     */
    protected void beforeUpdate() {
        // default no-op
    }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdateAt() { return updateAt; }
    public boolean isDeleted() { return deleted; }

    protected void markDeleted() { this.deleted = true; }
    protected void restore() { this.deleted = false; }
}