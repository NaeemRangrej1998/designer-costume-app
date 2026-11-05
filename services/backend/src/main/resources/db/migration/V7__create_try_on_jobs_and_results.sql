CREATE TABLE try_on_jobs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    user_photo_id UUID NOT NULL,
    costume_id UUID NOT NULL,
    costume_variant_id UUID,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    ai_model_name VARCHAR(150),
    ai_model_version VARCHAR(100),
    request_metadata JSONB NOT NULL DEFAULT '{}'::JSONB,
    error_message VARCHAR(4000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    CONSTRAINT ck_try_on_jobs_status
        CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED', 'CANCELLED')),
    CONSTRAINT ck_try_on_jobs_time_order
        CHECK (completed_at IS NULL OR started_at IS NULL OR completed_at >= started_at),
    CONSTRAINT fk_try_on_jobs_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT fk_try_on_jobs_user_photo FOREIGN KEY (user_photo_id)
        REFERENCES user_photos (id) ON DELETE RESTRICT,
    CONSTRAINT fk_try_on_jobs_costume FOREIGN KEY (costume_id)
        REFERENCES costumes (id) ON DELETE RESTRICT,
    CONSTRAINT fk_try_on_jobs_costume_variant FOREIGN KEY (costume_variant_id)
        REFERENCES costume_variants (id) ON DELETE SET NULL
);

CREATE INDEX idx_try_on_jobs_user_created ON try_on_jobs (user_id, created_at DESC);
CREATE INDEX idx_try_on_jobs_photo_id ON try_on_jobs (user_photo_id);
CREATE INDEX idx_try_on_jobs_costume_id ON try_on_jobs (costume_id);
CREATE INDEX idx_try_on_jobs_variant_id ON try_on_jobs (costume_variant_id);
CREATE INDEX idx_try_on_jobs_status_created ON try_on_jobs (status, created_at);

CREATE TABLE try_on_results (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    try_on_job_id UUID NOT NULL,
    result_image_url VARCHAR(2048) NOT NULL,
    storage_key VARCHAR(512) NOT NULL UNIQUE,
    display_order INTEGER NOT NULL DEFAULT 0,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_try_on_results_display_order CHECK (display_order >= 0),
    CONSTRAINT fk_try_on_results_job FOREIGN KEY (try_on_job_id)
        REFERENCES try_on_jobs (id) ON DELETE CASCADE
);

CREATE INDEX idx_try_on_results_job_order ON try_on_results (try_on_job_id, display_order);
CREATE UNIQUE INDEX uq_try_on_results_primary
    ON try_on_results (try_on_job_id) WHERE is_primary = TRUE;
