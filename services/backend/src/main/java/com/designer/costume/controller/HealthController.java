package com.designer.costume.controller;

import com.designer.costume.dto.response.ApiResponse;
import com.designer.costume.dto.HealthResponse;
import com.designer.costume.service.HealthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/health")
@Tag(name = "Health")
public class HealthController {

    private final HealthService healthService;

    public HealthController(HealthService healthService) {
        this.healthService = healthService;
    }

    @GetMapping
    @Operation(summary = "Check API liveness")
    public ApiResponse<HealthResponse> getHealth() {
        return ApiResponse.success("Backend is running", healthService.getHealth());
    }
}
