package com.designer.costume.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.util.UUID;

public record AdminCostumeRequest(
        @NotBlank @Size(max = 200) String name,
        @Size(max = 5000) String description,
        @NotBlank @Size(max = 80) String sku,
        @NotNull UUID categoryId,
        @NotNull @DecimalMin("0.00") BigDecimal basePrice,
        @Size(max = 2048)
                @Pattern(regexp = "^(https?://[^\\s]+|/api/v1/costumes/[0-9a-fA-F-]{36}/images/[0-9a-fA-F-]{36}/content)$")
                String primaryImageUrl,
        boolean active) {
}
