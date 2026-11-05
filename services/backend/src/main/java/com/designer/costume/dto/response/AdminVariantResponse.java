package com.designer.costume.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record AdminVariantResponse(
        UUID id,
        UUID costumeId,
        UUID colorId,
        String colorName,
        String colorHexCode,
        UUID sizeId,
        String sizeName,
        int sizeDisplayOrder,
        String sku,
        BigDecimal price,
        int stockQuantity,
        String imageUrl,
        boolean available) {
}
