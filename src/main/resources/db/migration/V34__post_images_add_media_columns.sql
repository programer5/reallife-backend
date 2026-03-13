ALTER TABLE post_images
    ADD COLUMN media_type VARCHAR(20) NOT NULL DEFAULT 'IMAGE',
    ADD COLUMN thumbnail_url VARCHAR(1000) NULL,
    ADD COLUMN content_type VARCHAR(100) NULL;

UPDATE post_images
SET media_type = 'IMAGE'
WHERE media_type IS NULL;
