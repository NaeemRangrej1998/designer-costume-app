ALTER TABLE costume_images
    ADD COLUMN storage_key VARCHAR(512),
    ADD COLUMN original_file_name VARCHAR(255),
    ADD COLUMN mime_type VARCHAR(100),
    ADD COLUMN file_size BIGINT,
    ADD COLUMN image_width INTEGER,
    ADD COLUMN image_height INTEGER;

CREATE UNIQUE INDEX uq_costume_images_storage_key
    ON costume_images (storage_key)
    WHERE storage_key IS NOT NULL;

ALTER TABLE costume_images
    ADD CONSTRAINT ck_costume_images_file_size
        CHECK (file_size IS NULL OR file_size > 0),
    ADD CONSTRAINT ck_costume_images_dimensions
        CHECK ((image_width IS NULL OR image_width > 0)
            AND (image_height IS NULL OR image_height > 0));
