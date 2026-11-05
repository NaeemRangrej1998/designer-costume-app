package com.designer.costume.service;

import com.designer.costume.dto.response.AdminInquiryResponse;
import com.designer.costume.dto.response.PageResponse;
import com.designer.costume.entity.inquiry.InquiryEntity;
import com.designer.costume.enums.InquiryStatus;
import com.designer.costume.exception.ResourceNotFoundException;
import com.designer.costume.repository.inquiry.InquiryRepository;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AdminInquiryService {

    private final InquiryRepository inquiryRepository;
    private final AdminActivityService activityService;

    public AdminInquiryService(
            InquiryRepository inquiryRepository,
            AdminActivityService activityService) {
        this.inquiryRepository = inquiryRepository;
        this.activityService = activityService;
    }

    public PageResponse<AdminInquiryResponse> getInquiries(
            int page, int size, String search, InquiryStatus status) {
        Page<InquiryEntity> result = inquiryRepository.findAdminInquiries(
                search == null || search.isBlank() ? "" : search.trim(),
                status,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return PageResponse.from(result.map(this::toResponse));
    }

    public AdminInquiryResponse getInquiry(UUID id) {
        return toResponse(findInquiry(id));
    }

    @Transactional
    public AdminInquiryResponse updateStatus(UUID id, InquiryStatus status) {
        InquiryEntity inquiry = findInquiry(id);
        inquiry.setStatus(status);
        InquiryEntity saved = inquiryRepository.save(inquiry);
        activityService.record("STATUS_UPDATED", "INQUIRY", saved.getId());
        return toResponse(saved);
    }

    private InquiryEntity findInquiry(UUID id) {
        return inquiryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inquiry", id));
    }

    private AdminInquiryResponse toResponse(InquiryEntity inquiry) {
        return new AdminInquiryResponse(
                inquiry.getId(),
                inquiry.getCustomerName(),
                inquiry.getEmail(),
                inquiry.getPhoneNumber(),
                inquiry.getMessage(),
                inquiry.getStatus(),
                inquiry.getCostume() == null ? null : inquiry.getCostume().getId(),
                inquiry.getCostume() == null ? null : inquiry.getCostume().getName(),
                inquiry.getCostumeVariant() == null ? null : inquiry.getCostumeVariant().getId(),
                inquiry.getCostumeVariant() == null ? null : inquiry.getCostumeVariant().getSku(),
                inquiry.getCreatedAt(),
                inquiry.getUpdatedAt());
    }
}
