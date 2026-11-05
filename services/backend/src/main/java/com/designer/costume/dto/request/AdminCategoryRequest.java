package com.designer.costume.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import java.util.UUID;

public record AdminCategoryRequest(
        @NotBlank @Size(max = 150) String name,
        @Size(max = 2000) String description,
        @Size(max = 2048) @Pattern(regexp = "^https?://[^\\s]+$") String imageUrl,
        UUID parentCategoryId,
        boolean active) {
}
