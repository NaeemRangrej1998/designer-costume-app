package com.designer.costume.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record CostumeVariantResponse(
        UUID id,
        UUID costumeId,
        String colorName,
        String colorHexCode,
        String sizeName,
        int sizeDisplayOrder,
        BigDecimal price,
        boolean available) {
}
