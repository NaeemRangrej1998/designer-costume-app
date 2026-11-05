package com.designer.costume.dto.response;

import java.util.UUID;

public record AdminCategoryResponse(
        UUID id,
        String name,
        String description,
        String imageUrl,
        UUID parentCategoryId,
        String parentCategoryName,
        boolean active) {
}
