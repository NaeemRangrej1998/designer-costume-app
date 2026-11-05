package com.designer.costume.service;

import com.designer.costume.dto.response.AdminActivityResponse;
import com.designer.costume.security.CustomUserDetails;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminActivityService {

    private final JdbcTemplate jdbcTemplate;

    public AdminActivityService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public void record(String action, String resourceType, UUID resourceId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("Authenticated administrator is required for activity recording");
        }
        UUID actorId = authentication.getPrincipal() instanceof CustomUserDetails userDetails
                ? userDetails.getId()
                : null;
        jdbcTemplate.update("""
                INSERT INTO admin_activity_events (actor_user_id, action, resource_type, resource_id)
                VALUES (?, ?, ?, ?)
                """, actorId, action, resourceType, resourceId);
    }

    @Transactional(readOnly = true)
    public List<AdminActivityResponse> getRecentActivity(int limit) {
        return jdbcTemplate.query("""
                SELECT event.id,
                       COALESCE(actor.full_name, 'Unknown administrator') AS actor_name,
                       event.action,
                       event.resource_type,
                       event.resource_id,
                       event.created_at
                FROM admin_activity_events event
                LEFT JOIN users actor ON actor.id = event.actor_user_id
                ORDER BY event.created_at DESC, event.id DESC
                LIMIT ?
                """,
                (resultSet, rowNumber) -> new AdminActivityResponse(
                        resultSet.getObject("id", UUID.class),
                        resultSet.getString("actor_name"),
                        resultSet.getString("action"),
                        resultSet.getString("resource_type"),
                        resultSet.getObject("resource_id", UUID.class),
                        resultSet.getTimestamp("created_at").toInstant()),
                limit);
    }
}
