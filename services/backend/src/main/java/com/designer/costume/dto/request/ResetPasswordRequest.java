package com.designer.costume.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank @Size(max = 256) String token,
        @NotBlank @PasswordPolicy String newPassword) {
}
