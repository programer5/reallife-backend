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

        // ✅ 서브클래스 훅
        prePersist();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updateAt = LocalDateTime.now();

        // ✅ 서브클래스 훅
        preUpdate();
    }

    // ✅ 기본은 아무것도 안 함. 필요하면 엔티티에서 override
    protected void prePersist() {}
    protected void preUpdate() {}

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdateAt() { return updateAt; }
    public boolean isDeleted() { return deleted; }

    protected void markDeleted() {
        this.deleted = true;
    }

    protected void restore() {
        this.deleted = false;
    }
}