package com.example.backend.domain.user;

import com.example.backend.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

import static jakarta.persistence.EnumType.STRING;

@Getter
@NoArgsConstructor
@Entity
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_users_email", columnNames = "email"),
                @UniqueConstraint(name = "uk_users_handle", columnNames = "handle")
        },
        indexes = {
                // ✅ 검색 성능용 인덱스
                @Index(name = "idx_users_handle_lower", columnList = "handle_lower"),
                @Index(name = "idx_users_name_lower", columnList = "name_lower"),
                @Index(name = "idx_users_handle_lower_id", columnList = "handle_lower, id")
        }
)
public class User extends BaseEntity {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String email;

    // ✅ 인스타 아이디(핸들) — 고유, 노출용
    @Column(nullable = false, length = 30, unique = true)
    private String handle;

    // ✅ 검색 최적화용 (항상 소문자)
    @Column(name = "handle_lower", nullable = false, length = 30)
    private String handleLower;

    @Column(nullable = false, length = 60)
    private String password;

    @Column(nullable = false, length = 30)
    private String name;

    // ✅ 검색 최적화용 (항상 소문자)
    @Column(name = "name_lower", nullable = false, length = 30)
    private String nameLower;

    // ✅ OAuth 확장용 (지금은 LOCAL)
    @Enumerated(STRING)
    @Column(nullable = false, length = 20)
    private AuthProvider provider = AuthProvider.LOCAL;

    // provider 계정의 고유 식별자(구글 sub 등). LOCAL이면 null.
    @Column(length = 100)
    private String providerId;

    // ✅ 팔로워 수 (성능 위해 캐시)
    @Column(nullable = false)
    private long followerCount = 0;

    public User(String email, String handle, String password, String name) {
        this.email = email;
        this.handle = handle;
        this.password = password;
        this.name = name;
        this.provider = AuthProvider.LOCAL;
        syncSearchFields();
    }

    @Override
    protected void beforePersist() {
        syncSearchFields();
    }

    @Override
    protected void beforeUpdate() {
        syncSearchFields();
    }

    private void syncSearchFields() {
        this.handleLower = (handle == null) ? "" : handle.toLowerCase();
        this.nameLower = (name == null) ? "" : name.toLowerCase();
    }

    public void increaseFollowerCount() {
        this.followerCount++;
    }

    public void decreaseFollowerCount() {
        if (this.followerCount > 0) this.followerCount--;
    }
}