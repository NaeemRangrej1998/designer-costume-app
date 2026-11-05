package com.designer.costume.service;

import com.designer.costume.dto.response.DashboardCategoryCountResponse;
import com.designer.costume.dto.response.DashboardResponse;
import com.designer.costume.enums.InquiryStatus;
import com.designer.costume.repository.category.CategoryRepository;
import com.designer.costume.repository.costume.CostumeRepository;
import com.designer.costume.repository.inquiry.InquiryRepository;
import com.designer.costume.repository.user.UserRepository;
import com.designer.costume.repository.variant.CostumeVariantRepository;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DashboardService {

    private final CategoryRepository categoryRepository;
    private final CostumeRepository costumeRepository;
    private final UserRepository userRepository;
    private final CostumeVariantRepository costumeVariantRepository;
    private final InquiryRepository inquiryRepository;

    public DashboardService(
            CategoryRepository categoryRepository,
            CostumeRepository costumeRepository,
            UserRepository userRepository,
            CostumeVariantRepository costumeVariantRepository,
            InquiryRepository inquiryRepository) {
        this.categoryRepository = categoryRepository;
        this.costumeRepository = costumeRepository;
        this.userRepository = userRepository;
        this.costumeVariantRepository = costumeVariantRepository;
        this.inquiryRepository = inquiryRepository;
    }

    public DashboardResponse getSummary(Set<String> permissions) {
        boolean canViewCategories = permissions.contains("CATEGORY_VIEW");
        boolean canViewCostumes = permissions.contains("COSTUME_VIEW");
        boolean canViewUsers = permissions.contains("USER_VIEW");
        boolean canViewInquiries = permissions.contains("INQUIRY_VIEW");

        List<DashboardCategoryCountResponse> distribution =
                canViewCategories && canViewCostumes
                        ? costumeRepository.countActiveCostumesByActiveCategory()
                        : null;

        return new DashboardResponse(
                canViewCategories ? categoryRepository.countByActiveTrue() : null,
                canViewCostumes ? costumeRepository.countByActiveTrue() : null,
                canViewUsers ? userRepository.countActiveCustomers() : null,
                canViewCostumes ? costumeVariantRepository.countAvailableVariantsForActiveCatalog() : null,
                canViewInquiries ? inquiryRepository.count() : null,
                canViewInquiries ? inquiryRepository.countByStatus(InquiryStatus.NEW) : null,
                distribution);
    }
}
