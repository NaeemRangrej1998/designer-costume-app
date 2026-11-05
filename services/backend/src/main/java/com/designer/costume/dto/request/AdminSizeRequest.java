package com.designer.costume.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminSizeRequest(
        @NotBlank @Size(max = 40) String name,
        int displayOrder,
        boolean active) {
}
