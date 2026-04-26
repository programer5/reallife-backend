ALTER TABLE posts
    ADD COLUMN latitude DOUBLE NULL,
    ADD COLUMN longitude DOUBLE NULL,
    ADD COLUMN place_name VARCHAR(120) NULL;

CREATE INDEX idx_posts_location_created ON posts (latitude, longitude, created_at);
