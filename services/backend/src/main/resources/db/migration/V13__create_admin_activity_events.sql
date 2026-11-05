CREATE TABLE admin_activity_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_user_id UUID,
    action VARCHAR(40) NOT NULL,
    resource_type VARCHAR(40) NOT NULL,
    resource_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_admin_activity_actor FOREIGN KEY (actor_user_id)
        REFERENCES users (id) ON DELETE SET NULL
);

CREATE INDEX idx_admin_activity_events_created_at
    ON admin_activity_events (created_at DESC);
