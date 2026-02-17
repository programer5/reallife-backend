ALTER TABLE users
    ADD COLUMN bio VARCHAR(255) NULL,
  ADD COLUMN website VARCHAR(255) NULL,
  ADD COLUMN profile_image_file_id BINARY(16) NULL;

ALTER TABLE users
    ADD CONSTRAINT fk_users_profile_image_file
        FOREIGN KEY (profile_image_file_id) REFERENCES uploaded_files(id);

CREATE INDEX idx_users_handle ON users(handle);