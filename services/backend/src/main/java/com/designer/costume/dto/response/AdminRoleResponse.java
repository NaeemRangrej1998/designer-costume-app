package com.designer.costume.dto.response;

import java.util.Set;
import java.util.UUID;

public record AdminRoleResponse(
        UUID id,
        String name,
        String description,
        Set<String> permissions) {
}
