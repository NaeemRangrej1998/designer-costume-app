package com.designer.costume.dto.response;

import com.designer.costume.enums.InquiryStatus;
import java.time.Instant;
import java.util.UUID;

public record AdminInquiryResponse(
        UUID id,
        String customerName,
        String email,
        String phoneNumber,
        String message,
        InquiryStatus status,
        UUID costumeId,
        String costumeName,
        UUID variantId,
        String variantSku,
        Instant createdAt,
        Instant updatedAt) {
}
