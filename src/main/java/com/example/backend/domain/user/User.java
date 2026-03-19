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

    @Column(nullable = false, length = 30, unique = true)
    private String handle;

    @Column(name = "handle_lower", nullable = false, length = 30)
    private String handleLower;

    @Column(nullable = false, length = 60)
    private String password;

    @Column(nullable = false, length = 30)
    private String name;

    @Column(name = "name_lower", nullable = false, length = 30)
    private String nameLower;

    @Enumerated(STRING)
    @Column(nullable = false, length = 20)
    private AuthProvider provider = AuthProvider.LOCAL;

    @Column(length = 100)
    private String providerId;

    @Column(nullable = false)
    private long followerCount = 0;

    @Column(length = 255)
    private String bio;

    @Column(length = 255)
    private String website;

    @Column(name = "profile_image_file_id", columnDefinition = "BINARY(16)")
    private UUID profileImageFileId;

    @Column(name = "pin_remind_browser_notify", nullable = false)
    private boolean pinRemindBrowserNotify = false;

    @Column(name = "pin_remind_sound", nullable = false)
    private boolean pinRemindSound = false;

    @Column(name = "pin_remind_vibrate", nullable = false)
    private boolean pinRemindVibrate = false;

    public User(String email, String handle, String password, String name) {
        this.email = email;
        this.handle = handle;
        this.password = password;
        this.name = name;
        this.provider = AuthProvider.LOCAL;

        syncSearchFields(); // 생성자에서도 안전하게
    }

    public void updateProfile(String name, String bio, String website, UUID profileImageFileId) {
        if (name != null && !name.isBlank()) {
            this.name = name.trim();
        }
        this.bio = bio;
        this.website = website;
        this.profileImageFileId = profileImageFileId;
    }


    public void updateReminderSettings(Boolean pinRemindBrowserNotify, Boolean pinRemindSound, Boolean pinRemindVibrate) {
        if (pinRemindBrowserNotify != null) {
            this.pinRemindBrowserNotify = pinRemindBrowserNotify;
        }
        if (pinRemindSound != null) {
            this.pinRemindSound = pinRemindSound;
        }
        if (pinRemindVibrate != null) {
            this.pinRemindVibrate = pinRemindVibrate;
        }
    }

    // ✅ BaseEntity 훅에서 호출됨
    @Override
    protected void prePersist() {
        syncSearchFields();
    }

    @Override
    protected void preUpdate() {
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