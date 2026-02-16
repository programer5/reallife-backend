-- post_images에 file_id 추가 (기존 imageUrl은 유지해서 호환)
ALTER TABLE post_images
    ADD COLUMN file_id BINARY(16) NULL AFTER post_id;

-- post_id + sort_order 조회 최적화 (이미 있으면 에러날 수 있으니 없을 때만 적용 권장)
CREATE INDEX idx_post_images_post_id_sort ON post_images(post_id, sort_order);

-- uploaded_files FK
ALTER TABLE post_images
    ADD CONSTRAINT fk_post_images_file
        FOREIGN KEY (file_id) REFERENCES uploaded_files(id);