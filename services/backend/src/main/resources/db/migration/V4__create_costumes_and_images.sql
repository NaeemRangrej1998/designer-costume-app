CREATE TABLE costumes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(200) NOT NULL,
    description VARCHAR(5000),
    sku VARCHAR(80) NOT NULL UNIQUE,
    category_id UUID NOT NULL,
    base_price NUMERIC(12, 2) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_costumes_base_price_nonnegative CHECK (base_price >= 0),
    CONSTRAINT fk_costumes_category FOREIGN KEY (category_id)
        REFERENCES categories (id) ON DELETE RESTRICT
);

CREATE INDEX idx_costumes_category_id ON costumes (category_id);
CREATE INDEX idx_costumes_active_name ON costumes (is_active, name);
CREATE INDEX idx_costumes_name_lower ON costumes (LOWER(name));

CREATE TABLE costume_images (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    costume_id UUID NOT NULL,
    image_url VARCHAR(2048) NOT NULL,
    image_type VARCHAR(20) NOT NULL,
    display_order INTEGER NOT NULL DEFAULT 0,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_costume_images_type CHECK (image_type IN ('PRODUCT', 'FRONT', 'BACK', 'DETAIL')),
    CONSTRAINT ck_costume_images_display_order CHECK (display_order >= 0),
    CONSTRAINT fk_costume_images_costume FOREIGN KEY (costume_id)
        REFERENCES costumes (id) ON DELETE CASCADE
);

CREATE INDEX idx_costume_images_costume_order ON costume_images (costume_id, display_order);
CREATE UNIQUE INDEX uq_costume_images_primary
    ON costume_images (costume_id) WHERE is_primary = TRUE;
