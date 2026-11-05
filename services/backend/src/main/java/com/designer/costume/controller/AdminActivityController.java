package com.designer.costume.controller;

import com.designer.costume.dto.response.AdminActivityResponse;
import com.designer.costume.dto.response.ApiResponse;
import com.designer.costume.service.AdminActivityService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@Validated
@RestController
@RequestMapping("/api/v1/admin/activity")
@SecurityRequirement(name = "bearerAuth")
public class AdminActivityController {

    private final AdminActivityService activityService;

    public AdminActivityController(AdminActivityService activityService) {
        this.activityService = activityService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('USER_VIEW')")
    public ApiResponse<List<AdminActivityResponse>> getRecentActivity(
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int limit) {
        return ApiResponse.success("Recent activity fetched successfully",
                activityService.getRecentActivity(limit));
    }
}
