package com.designer.costume.dto.request;

import com.designer.costume.enums.InquiryStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateInquiryStatusRequest(@NotNull InquiryStatus status) {
}
