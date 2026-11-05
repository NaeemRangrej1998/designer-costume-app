CREATE TABLE colors (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(80) NOT NULL,
    hex_code VARCHAR(7) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_colors_hex_code CHECK (hex_code ~ '^#[0-9A-Fa-f]{6}$')
);

CREATE UNIQUE INDEX uq_colors_name_lower ON colors (LOWER(name));

CREATE TABLE sizes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(40) NOT NULL UNIQUE,
    display_order INTEGER NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_sizes_display_order CHECK (display_order >= 0)
);

CREATE UNIQUE INDEX uq_sizes_display_order ON sizes (display_order);

CREATE TABLE costume_variants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    costume_id UUID NOT NULL,
    color_id UUID NOT NULL,
    size_id UUID NOT NULL,
    price NUMERIC(12, 2) NOT NULL,
    stock_quantity INTEGER NOT NULL DEFAULT 0,
    sku VARCHAR(100) NOT NULL UNIQUE,
    is_available BOOLEAN NOT NULL DEFAULT TRUE,
    image_url VARCHAR(2048),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_costume_variant_combination UNIQUE (costume_id, color_id, size_id),
    CONSTRAINT ck_costume_variants_price_nonnegative CHECK (price >= 0),
    CONSTRAINT ck_costume_variants_stock_nonnegative CHECK (stock_quantity >= 0),
    CONSTRAINT fk_costume_variants_costume FOREIGN KEY (costume_id)
        REFERENCES costumes (id) ON DELETE RESTRICT,
    CONSTRAINT fk_costume_variants_color FOREIGN KEY (color_id)
        REFERENCES colors (id) ON DELETE RESTRICT,
    CONSTRAINT fk_costume_variants_size FOREIGN KEY (size_id)
        REFERENCES sizes (id) ON DELETE RESTRICT
);

CREATE INDEX idx_costume_variants_costume_id ON costume_variants (costume_id);
CREATE INDEX idx_costume_variants_color_size ON costume_variants (color_id, size_id);
CREATE INDEX idx_costume_variants_size_id ON costume_variants (size_id);
CREATE INDEX idx_costume_variants_available ON costume_variants (is_available);
