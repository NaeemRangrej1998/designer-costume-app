CREATE TABLE permissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(500),
    module VARCHAR(60) NOT NULL,
    action VARCHAR(40) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_permissions_module_action UNIQUE (module, action)
);

CREATE INDEX idx_permissions_module ON permissions (module);

CREATE TABLE role_permissions (
    role_id UUID NOT NULL,
    permission_id UUID NOT NULL,
    CONSTRAINT pk_role_permissions PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_role_permissions_role FOREIGN KEY (role_id)
        REFERENCES roles (id) ON DELETE CASCADE,
    CONSTRAINT fk_role_permissions_permission FOREIGN KEY (permission_id)
        REFERENCES permissions (id) ON DELETE CASCADE
);

CREATE INDEX idx_role_permissions_permission_id ON role_permissions (permission_id);

INSERT INTO permissions (name, description, module, action) VALUES
    ('USER_VIEW', 'View users', 'USER', 'VIEW'),
    ('USER_CREATE', 'Create users', 'USER', 'CREATE'),
    ('USER_UPDATE', 'Update users', 'USER', 'UPDATE'),
    ('USER_DELETE', 'Delete users', 'USER', 'DELETE'),
    ('CATEGORY_VIEW', 'View categories', 'CATEGORY', 'VIEW'),
    ('CATEGORY_CREATE', 'Create categories', 'CATEGORY', 'CREATE'),
    ('CATEGORY_UPDATE', 'Update categories', 'CATEGORY', 'UPDATE'),
    ('CATEGORY_DELETE', 'Delete categories', 'CATEGORY', 'DELETE'),
    ('COSTUME_VIEW', 'View costumes', 'COSTUME', 'VIEW'),
    ('COSTUME_CREATE', 'Create costumes', 'COSTUME', 'CREATE'),
    ('COSTUME_UPDATE', 'Update costumes', 'COSTUME', 'UPDATE'),
    ('COSTUME_DELETE', 'Delete costumes', 'COSTUME', 'DELETE'),
    ('INQUIRY_VIEW', 'View customer inquiries', 'INQUIRY', 'VIEW'),
    ('INQUIRY_UPDATE', 'Update customer inquiries', 'INQUIRY', 'UPDATE'),
    ('TRYON_VIEW', 'View virtual try-on jobs', 'TRYON', 'VIEW');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.name = 'SUPER_ADMIN';

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.name = 'ADMIN'
  AND p.name <> 'USER_DELETE';
