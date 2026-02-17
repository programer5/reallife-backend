ALTER TABLE uploaded_files
    ADD COLUMN thumbnail_file_key VARCHAR(500) NULL,
    ADD COLUMN thumbnail_content_type VARCHAR(100) NULL,
    ADD COLUMN thumbnail_size BIGINT NULL;