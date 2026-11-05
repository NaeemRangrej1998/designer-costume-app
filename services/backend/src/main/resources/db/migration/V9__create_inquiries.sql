CREATE TABLE inquiries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID,
    costume_id UUID,
    costume_variant_id UUID,
    customer_name VARCHAR(150) NOT NULL,
    email VARCHAR(320) NOT NULL,
    phone_number VARCHAR(32),
    message VARCHAR(5000) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'NEW',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_inquiries_status
        CHECK (status IN ('NEW', 'CONTACTED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED')),
    CONSTRAINT fk_inquiries_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT fk_inquiries_costume FOREIGN KEY (costume_id)
        REFERENCES costumes (id) ON DELETE SET NULL,
    CONSTRAINT fk_inquiries_costume_variant FOREIGN KEY (costume_variant_id)
        REFERENCES costume_variants (id) ON DELETE SET NULL
);

CREATE INDEX idx_inquiries_status_created ON inquiries (status, created_at);
CREATE INDEX idx_inquiries_user_id ON inquiries (user_id);
CREATE INDEX idx_inquiries_costume_id ON inquiries (costume_id);
CREATE INDEX idx_inquiries_variant_id ON inquiries (costume_variant_id);
CREATE INDEX idx_inquiries_email_lower ON inquiries (LOWER(email));
