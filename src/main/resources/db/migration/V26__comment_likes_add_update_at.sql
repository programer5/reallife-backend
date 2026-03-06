-- comment_likes 테이블에 update_at 컬럼 추가 (BaseEntity 매핑 대응)
ALTER TABLE comment_likes
    ADD COLUMN update_at DATETIME(6) NOT NULL
    DEFAULT CURRENT_TIMESTAMP(6)
    ON UPDATE CURRENT_TIMESTAMP(6);