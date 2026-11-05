CREATE TABLE user_role_permission_grants (
    user_id UUID NOT NULL,
    role_id UUID NOT NULL,
    permission_id UUID NOT NULL,
    CONSTRAINT pk_user_role_permission_grants
        PRIMARY KEY (user_id, role_id, permission_id),
    CONSTRAINT fk_user_role_permission_grants_user_role
        FOREIGN KEY (user_id, role_id)
        REFERENCES user_roles (user_id, role_id) ON DELETE CASCADE,
    CONSTRAINT fk_user_role_permission_grants_permission
        FOREIGN KEY (permission_id)
        REFERENCES permissions (id) ON DELETE CASCADE
);

INSERT INTO user_role_permission_grants (user_id, role_id, permission_id)
SELECT user_role.user_id, user_role.role_id, role_permission.permission_id
FROM user_roles user_role
JOIN role_permissions role_permission ON role_permission.role_id = user_role.role_id
ON CONFLICT DO NOTHING;
