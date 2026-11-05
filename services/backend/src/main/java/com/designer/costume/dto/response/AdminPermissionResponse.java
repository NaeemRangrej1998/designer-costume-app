package com.designer.costume.dto.response;

import java.util.UUID;

public record AdminPermissionResponse(
        UUID id,
        String name,
        String description,
        String module,
        String action) {
}
