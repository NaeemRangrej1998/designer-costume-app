package com.designer.costume.repository.user;

import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class UserRolePermissionGrantRepository {

    private static final String SNAPSHOT_ROLE_PERMISSIONS = """
            INSERT INTO user_role_permission_grants (user_id, role_id, permission_id)
            SELECT user_role.user_id, user_role.role_id, role_permission.permission_id
            FROM user_roles user_role
            JOIN role_permissions role_permission ON role_permission.role_id = user_role.role_id
            WHERE user_role.user_id = ?
            ON CONFLICT DO NOTHING
            """;

    private static final String SNAPSHOT_ROLE_PERMISSIONS_FOR_ROLE = """
            INSERT INTO user_role_permission_grants (user_id, role_id, permission_id)
            SELECT user_role.user_id, user_role.role_id, role_permission.permission_id
            FROM user_roles user_role
            JOIN role_permissions role_permission ON role_permission.role_id = user_role.role_id
            WHERE user_role.user_id = ? AND user_role.role_id = ?
            ON CONFLICT DO NOTHING
            """;

    private static final String FIND_EFFECTIVE_PERMISSIONS = """
            SELECT DISTINCT permission.name
            FROM user_roles user_role
            JOIN user_role_permission_grants grant_row
              ON grant_row.user_id = user_role.user_id
             AND grant_row.role_id = user_role.role_id
            JOIN role_permissions role_permission
              ON role_permission.role_id = user_role.role_id
             AND role_permission.permission_id = grant_row.permission_id
            JOIN permissions permission ON permission.id = grant_row.permission_id
            WHERE user_role.user_id = ?
            """;

    private final JdbcTemplate jdbcTemplate;

    public UserRolePermissionGrantRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void snapshotCurrentRolePermissions(UUID userId) {
        jdbcTemplate.update(SNAPSHOT_ROLE_PERMISSIONS, userId);
    }

    public void snapshotRolePermissions(UUID userId, UUID roleId) {
        jdbcTemplate.update(SNAPSHOT_ROLE_PERMISSIONS_FOR_ROLE, userId, roleId);
    }

    public int removePermissionsNoLongerGrantedByRole(UUID roleId) {
        return jdbcTemplate.update("""
                DELETE FROM user_role_permission_grants grant_row
                WHERE grant_row.role_id = ?
                  AND NOT EXISTS (
                      SELECT 1
                      FROM role_permissions role_permission
                      WHERE role_permission.role_id = grant_row.role_id
                        AND role_permission.permission_id = grant_row.permission_id
                  )
                """, roleId);
    }

    public Set<String> findEffectivePermissions(UUID userId) {
        return Set.copyOf(jdbcTemplate.queryForList(
                FIND_EFFECTIVE_PERMISSIONS, String.class, userId));
    }
}
