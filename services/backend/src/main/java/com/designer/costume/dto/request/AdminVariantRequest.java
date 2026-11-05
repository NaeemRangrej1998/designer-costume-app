package com.designer.costume.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.util.UUID;

public record AdminVariantRequest(
        @NotNull UUID colorId,
        @NotNull UUID sizeId,
        @NotBlank @Size(max = 100) String sku,
        @NotNull @DecimalMin("0.00") BigDecimal price,
        @PositiveOrZero int stockQuantity,
        @Size(max = 2048) @Pattern(regexp = "^https?://[^\\s]+$") String imageUrl,
        boolean available) {
}
