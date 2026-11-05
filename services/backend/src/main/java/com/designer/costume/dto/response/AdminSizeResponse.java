package com.designer.costume.dto.response;

import java.util.UUID;

public record AdminSizeResponse(UUID id, String name, int displayOrder, boolean active) {
}
