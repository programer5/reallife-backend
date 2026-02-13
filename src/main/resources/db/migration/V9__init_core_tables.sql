-- V9__init_core_tables.sql
-- ddl-auto=validate 환경에서 부팅 가능하도록, 엔티티 기준 핵심 테이블들을 생성한다.

-- 1) users (BaseEntity 포함: created_at, update_at, deleted)
CREATE TABLE IF NOT EXISTS users (
                                     id BINARY(16) NOT NULL,
    email VARCHAR(100) NOT NULL,
    handle VARCHAR(30) NOT NULL,
    handle_lower VARCHAR(30) NOT NULL,
    password VARCHAR(60) NOT NULL,
    name VARCHAR(30) NOT NULL,
    name_lower VARCHAR(30) NOT NULL,
    provider VARCHAR(20) NOT NULL,
    provider_id VARCHAR(100) NULL,
    follower_count BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    update_at DATETIME(6) NOT NULL,
    deleted TINYINT(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT uk_users_handle UNIQUE (handle),
    INDEX idx_users_handle_lower (handle_lower),
    INDEX idx_users_name_lower (name_lower),
    INDEX idx_users_handle_lower_id (handle_lower, id)
    );

-- 2) posts (BaseEntity 포함)
CREATE TABLE IF NOT EXISTS posts (
                                     id BINARY(16) NOT NULL,
    author_id BINARY(16) NOT NULL,
    content TEXT NOT NULL,
    visibility VARCHAR(255) NOT NULL,
    like_count BIGINT NOT NULL DEFAULT 0,
    comment_count BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    update_at DATETIME(6) NOT NULL,
    deleted TINYINT(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
    );

-- 3) post_images (BaseEntity 없음)
CREATE TABLE IF NOT EXISTS post_images (
                                           id BINARY(16) NOT NULL,
    post_id BINARY(16) NOT NULL,
    image_url VARCHAR(1000) NOT NULL,
    sort_order INT NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_post_images_post_id (post_id),
    CONSTRAINT fk_post_images_post_id FOREIGN KEY (post_id) REFERENCES posts(id)
    );

-- 4) post_likes (BaseEntity 포함)
CREATE TABLE IF NOT EXISTS post_likes (
                                          id BINARY(16) NOT NULL,
    post_id BINARY(16) NOT NULL,
    user_id BINARY(16) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    update_at DATETIME(6) NOT NULL,
    deleted TINYINT(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_post_user_like UNIQUE (post_id, user_id),
    INDEX idx_post_id (post_id),
    INDEX idx_post_like_user_id (user_id)
    );

-- 5) uploaded_files (BaseEntity 포함)
CREATE TABLE IF NOT EXISTS uploaded_files (
                                              id BINARY(16) NOT NULL,
    uploader_id BINARY(16) NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    file_key VARCHAR(500) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    size BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    update_at DATETIME(6) NOT NULL,
    deleted TINYINT(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    INDEX idx_uploaded_uploader (uploader_id)
    );

-- 6) follows (BaseEntity 포함)
CREATE TABLE IF NOT EXISTS follows (
                                       id BINARY(16) NOT NULL,
    follower_id BINARY(16) NOT NULL,
    following_id BINARY(16) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    update_at DATETIME(6) NOT NULL,
    deleted TINYINT(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_follower_following UNIQUE (follower_id, following_id),
    INDEX idx_follower_id (follower_id),
    INDEX idx_following_id (following_id)
    );