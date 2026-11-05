INSERT INTO role_permissions (role_id, permission_id)
SELECT role_row.id, permission_row.id
FROM roles role_row
CROSS JOIN permissions permission_row
WHERE role_row.name = 'CUSTOMER'
  AND permission_row.name IN ('CATEGORY_VIEW', 'COSTUME_VIEW')
ON CONFLICT DO NOTHING;
