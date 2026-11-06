package com.designer.costume.dto.response;

import java.time.Instant;
import java.util.UUID;

public record UserPhotoResponse(
        UUID id,
        String originalFileName,
        String mimeType,
        long fileSize,
        Integer imageWidth,
        Integer imageHeight,
        String processingStatus,
        Instant createdAt,
        String contentUrl) {
}
