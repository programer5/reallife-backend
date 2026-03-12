ALTER TABLE conversations
    ADD COLUMN title VARCHAR(255) NULL,
    ADD COLUMN owner_id BINARY(16) NULL,
    ADD COLUMN cover_image_file_id BINARY(16) NULL;