CREATE TABLE favorites (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    costume_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_favorites_user_costume UNIQUE (user_id, costume_id),
    CONSTRAINT fk_favorites_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_favorites_costume FOREIGN KEY (costume_id)
        REFERENCES costumes (id) ON DELETE CASCADE
);

CREATE INDEX idx_favorites_costume_id ON favorites (costume_id);
CREATE INDEX idx_favorites_user_created ON favorites (user_id, created_at DESC);
