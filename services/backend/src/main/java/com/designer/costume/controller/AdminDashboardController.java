package com.designer.costume.controller;

import com.designer.costume.dto.response.ApiResponse;
import com.designer.costume.dto.response.DashboardResponse;
import com.designer.costume.security.CustomUserDetails;
import com.designer.costume.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/dashboard")
@Tag(name = "Admin dashboard", description = "Permission-filtered dashboard summary")
public class AdminDashboardController {

    private final DashboardService dashboardService;

    public AdminDashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get dashboard metrics available to the current administrator")
    public ApiResponse<DashboardResponse> getDashboard(
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ApiResponse.success(
                "Dashboard summary fetched successfully",
                dashboardService.getSummary(principal.getPermissionNames()));
    }
}
