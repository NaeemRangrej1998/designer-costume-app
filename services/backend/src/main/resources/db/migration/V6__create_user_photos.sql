CREATE TABLE user_photos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    image_url VARCHAR(2048) NOT NULL,
    storage_key VARCHAR(512) NOT NULL UNIQUE,
    original_file_name VARCHAR(255) NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    image_width INTEGER,
    image_height INTEGER,
    processing_status VARCHAR(20) NOT NULL DEFAULT 'UPLOADED',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT ck_user_photos_file_size CHECK (file_size > 0),
    CONSTRAINT ck_user_photos_dimensions CHECK (
        (image_width IS NULL OR image_width > 0)
        AND (image_height IS NULL OR image_height > 0)
    ),
    CONSTRAINT ck_user_photos_processing_status
        CHECK (processing_status IN ('UPLOADED', 'PROCESSING', 'READY', 'FAILED')),
    CONSTRAINT fk_user_photos_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE RESTRICT
);

CREATE INDEX idx_user_photos_user_created ON user_photos (user_id, created_at DESC);
CREATE INDEX idx_user_photos_status ON user_photos (processing_status);
CREATE INDEX idx_user_photos_deleted_at ON user_photos (deleted_at);
