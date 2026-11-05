package com.designer.costume.controller;

import com.designer.costume.dto.response.ApiResponse;
import com.designer.costume.dto.response.CostumeResponse;
import com.designer.costume.dto.response.CostumeVariantResponse;
import com.designer.costume.dto.response.PageResponse;
import com.designer.costume.service.CostumeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/costumes")
@Tag(name = "Costumes", description = "Read active costumes and available variants")
public class CostumeController {

    private final CostumeService costumeService;

    public CostumeController(CostumeService costumeService) {
        this.costumeService = costumeService;
    }

    @GetMapping
    @Operation(summary = "List active costumes")
    public ApiResponse<PageResponse<CostumeResponse>> getCostumes(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
            @RequestParam(required = false) @Size(max = 100) String search,
            @RequestParam(required = false) UUID categoryId) {
        return ApiResponse.success(
                "Costumes fetched successfully",
                costumeService.getActiveCostumes(page, size, search, categoryId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an active costume by UUID")
    public ApiResponse<CostumeResponse> getCostume(@PathVariable UUID id) {
        return ApiResponse.success(
                "Costume fetched successfully",
                costumeService.getActiveCostume(id));
    }

    @GetMapping("/{costumeId}/variants")
    @Operation(summary = "List available variants for an active costume")
    public ApiResponse<List<CostumeVariantResponse>> getAvailableVariants(
            @PathVariable UUID costumeId) {
        return ApiResponse.success(
                "Available costume variants fetched successfully",
                costumeService.getAvailableVariants(costumeId));
    }
}
