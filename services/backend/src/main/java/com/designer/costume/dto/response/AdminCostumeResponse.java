package com.designer.costume.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record AdminCostumeResponse(
        UUID id,
        String name,
        String description,
        String sku,
        BigDecimal basePrice,
        UUID categoryId,
        String categoryName,
        String primaryImageUrl,
        boolean active) {
}
