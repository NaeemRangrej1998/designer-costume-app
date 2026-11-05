package com.designer.costume.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.Set;

public record AdminUpdateUserRequest(
        @NotBlank @Size(max = 150) String fullName,
        @NotBlank @Email @Size(max = 320) String email,
        @Size(max = 32) String phoneNumber,
        @NotEmpty Set<@NotBlank @Size(max = 80) String> roles,
        boolean active) {
}
