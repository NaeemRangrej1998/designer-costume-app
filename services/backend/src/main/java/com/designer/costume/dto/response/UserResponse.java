package com.designer.costume.dto.response;

import java.util.Set;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String fullName,
        String email,
        String phoneNumber,
        String profileImage,
        boolean active,
        Set<String> roles,
        Set<String> permissions) {
}
