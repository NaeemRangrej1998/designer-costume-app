package com.designer.costume.controller;

import com.designer.costume.dto.request.UpdateInquiryStatusRequest;
import com.designer.costume.dto.response.AdminInquiryResponse;
import com.designer.costume.dto.response.ApiResponse;
import com.designer.costume.dto.response.PageResponse;
import com.designer.costume.enums.InquiryStatus;
import com.designer.costume.service.AdminInquiryService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/admin/inquiries")
@SecurityRequirement(name = "bearerAuth")
public class AdminInquiryController {

    private final AdminInquiryService inquiryService;

    public AdminInquiryController(AdminInquiryService inquiryService) {
        this.inquiryService = inquiryService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('INQUIRY_VIEW')")
    public ApiResponse<PageResponse<AdminInquiryResponse>> getInquiries(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
            @RequestParam(required = false) @Size(max = 100) String search,
            @RequestParam(required = false) InquiryStatus status) {
        return ApiResponse.success("Inquiries fetched successfully",
                inquiryService.getInquiries(page, size, search, status));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('INQUIRY_VIEW')")
    public ApiResponse<AdminInquiryResponse> getInquiry(@PathVariable UUID id) {
        return ApiResponse.success("Inquiry fetched successfully", inquiryService.getInquiry(id));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('INQUIRY_UPDATE')")
    public ApiResponse<AdminInquiryResponse> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateInquiryStatusRequest request) {
        return ApiResponse.success("Inquiry status updated successfully",
                inquiryService.updateStatus(id, request.status()));
    }
}
