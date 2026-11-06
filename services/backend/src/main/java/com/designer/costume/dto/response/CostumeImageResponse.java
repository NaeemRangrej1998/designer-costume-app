package com.designer.costume.dto.response;

import java.time.Instant;
import java.util.UUID;

public record CostumeImageResponse(
        UUID id,
        String imageUrl,
        String originalFileName,
        String mimeType,
        Long fileSize,
        Integer imageWidth,
        Integer imageHeight,
        int displayOrder,
        boolean primary,
        Instant createdAt) {
}
