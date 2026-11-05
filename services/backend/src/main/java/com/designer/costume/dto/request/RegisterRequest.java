package com.designer.costume.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(max = 150) String fullName,
        @NotBlank @Email @Size(max = 320) String email,
        @Size(max = 32) String phoneNumber,
        @NotBlank @PasswordPolicy String password) {
}
