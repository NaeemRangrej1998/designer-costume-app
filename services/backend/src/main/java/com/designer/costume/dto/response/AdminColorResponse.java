package com.designer.costume.dto.response;

import java.util.UUID;

public record AdminColorResponse(UUID id, String name, String hexCode, boolean active) {
}
