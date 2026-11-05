package com.designer.costume.dto.response;

import java.util.List;

public record DashboardResponse(
        Long activeCategories,
        Long activeCostumes,
        Long activeCustomers,
        Long availableVariants,
        Long totalInquiries,
        Long newInquiries,
        List<DashboardCategoryCountResponse> costumeDistribution) {
}
