package com.designer.costume.dto.request;

import jakarta.validation.constraints.Size;

public record RefreshTokenRequest(@Size(max = 4096) String refreshToken) {
}
