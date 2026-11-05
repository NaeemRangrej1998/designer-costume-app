CREATE TABLE categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(150) NOT NULL,
    description VARCHAR(2000),
    image_url VARCHAR(2048),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    parent_category_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_categories_parent FOREIGN KEY (parent_category_id)
        REFERENCES categories (id) ON DELETE RESTRICT
);

CREATE UNIQUE INDEX uq_categories_name_lower ON categories (LOWER(name));
CREATE INDEX idx_categories_parent_category_id ON categories (parent_category_id);
CREATE INDEX idx_categories_active_name ON categories (is_active, name);
