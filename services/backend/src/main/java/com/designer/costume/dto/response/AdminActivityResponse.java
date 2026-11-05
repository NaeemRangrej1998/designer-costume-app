package com.designer.costume.dto.response;

import java.time.Instant;
import java.util.UUID;

public record AdminActivityResponse(
        UUID id,
        String actorName,
        String action,
        String resourceType,
        UUID resourceId,
        Instant createdAt) {
}
