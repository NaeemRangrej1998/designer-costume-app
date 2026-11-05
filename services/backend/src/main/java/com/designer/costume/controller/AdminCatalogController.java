package com.designer.costume.controller;

import com.designer.costume.dto.request.AdminCategoryRequest;
import com.designer.costume.dto.request.AdminColorRequest;
import com.designer.costume.dto.request.AdminCostumeRequest;
import com.designer.costume.dto.request.AdminSizeRequest;
import com.designer.costume.dto.request.AdminVariantRequest;
import com.designer.costume.dto.response.AdminCategoryResponse;
import com.designer.costume.dto.response.AdminColorResponse;
import com.designer.costume.dto.response.AdminCostumeResponse;
import com.designer.costume.dto.response.AdminSizeResponse;
import com.designer.costume.dto.response.AdminVariantResponse;
import com.designer.costume.dto.response.ApiResponse;
import com.designer.costume.dto.response.PageResponse;
import com.designer.costume.service.AdminCatalogService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/admin")
@SecurityRequirement(name = "bearerAuth")
public class AdminCatalogController {

    private final AdminCatalogService adminCatalogService;

    public AdminCatalogController(AdminCatalogService adminCatalogService) {
        this.adminCatalogService = adminCatalogService;
    }

    @GetMapping("/categories")
    @PreAuthorize("hasAuthority('CATEGORY_VIEW')")
    public ApiResponse<PageResponse<AdminCategoryResponse>> getCategories(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
            @RequestParam(required = false) @Size(max = 100) String search,
            @RequestParam(required = false) Boolean active) {
        return ApiResponse.success("Categories fetched successfully",
                adminCatalogService.getCategories(page, size, search, active));
    }

    @GetMapping("/categories/{id}")
    @PreAuthorize("hasAuthority('CATEGORY_VIEW')")
    public ApiResponse<AdminCategoryResponse> getCategory(@PathVariable UUID id) {
        return ApiResponse.success("Category fetched successfully", adminCatalogService.getCategory(id));
    }

    @PostMapping("/categories")
    @PreAuthorize("hasAuthority('CATEGORY_CREATE')")
    public ApiResponse<AdminCategoryResponse> createCategory(
            @Valid @RequestBody AdminCategoryRequest request) {
        return ApiResponse.success("Category created successfully",
                adminCatalogService.createCategory(request));
    }

    @PutMapping("/categories/{id}")
    @PreAuthorize("hasAuthority('CATEGORY_UPDATE')")
    public ApiResponse<AdminCategoryResponse> updateCategory(
            @PathVariable UUID id, @Valid @RequestBody AdminCategoryRequest request) {
        return ApiResponse.success("Category updated successfully",
                adminCatalogService.updateCategory(id, request));
    }

    @DeleteMapping("/categories/{id}")
    @PreAuthorize("hasAuthority('CATEGORY_DELETE')")
    public ApiResponse<Void> deactivateCategory(@PathVariable UUID id) {
        adminCatalogService.deactivateCategory(id);
        return ApiResponse.success("Category deactivated successfully", null);
    }

    @GetMapping("/costumes")
    @PreAuthorize("hasAuthority('COSTUME_VIEW')")
    public ApiResponse<PageResponse<AdminCostumeResponse>> getCostumes(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
            @RequestParam(required = false) @Size(max = 100) String search,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) Boolean active) {
        return ApiResponse.success("Costumes fetched successfully",
                adminCatalogService.getCostumes(page, size, search, categoryId, active));
    }

    @GetMapping("/costumes/{id}")
    @PreAuthorize("hasAuthority('COSTUME_VIEW')")
    public ApiResponse<AdminCostumeResponse> getCostume(@PathVariable UUID id) {
        return ApiResponse.success("Costume fetched successfully", adminCatalogService.getCostume(id));
    }

    @PostMapping("/costumes")
    @PreAuthorize("hasAuthority('COSTUME_CREATE')")
    public ApiResponse<AdminCostumeResponse> createCostume(
            @Valid @RequestBody AdminCostumeRequest request) {
        return ApiResponse.success("Costume created successfully",
                adminCatalogService.createCostume(request));
    }

    @PutMapping("/costumes/{id}")
    @PreAuthorize("hasAuthority('COSTUME_UPDATE')")
    public ApiResponse<AdminCostumeResponse> updateCostume(
            @PathVariable UUID id, @Valid @RequestBody AdminCostumeRequest request) {
        return ApiResponse.success("Costume updated successfully",
                adminCatalogService.updateCostume(id, request));
    }

    @DeleteMapping("/costumes/{id}")
    @PreAuthorize("hasAuthority('COSTUME_DELETE')")
    public ApiResponse<Void> deactivateCostume(@PathVariable UUID id) {
        adminCatalogService.deactivateCostume(id);
        return ApiResponse.success("Costume deactivated successfully", null);
    }

    @GetMapping("/colors")
    @PreAuthorize("hasAuthority('COSTUME_VIEW')")
    public ApiResponse<List<AdminColorResponse>> getColors() {
        return ApiResponse.success("Colors fetched successfully", adminCatalogService.getColors());
    }

    @PostMapping("/colors")
    @PreAuthorize("hasAuthority('COSTUME_CREATE')")
    public ApiResponse<AdminColorResponse> createColor(@Valid @RequestBody AdminColorRequest request) {
        return ApiResponse.success("Color created successfully", adminCatalogService.createColor(request));
    }

    @PutMapping("/colors/{id}")
    @PreAuthorize("hasAuthority('COSTUME_UPDATE')")
    public ApiResponse<AdminColorResponse> updateColor(
            @PathVariable UUID id, @Valid @RequestBody AdminColorRequest request) {
        return ApiResponse.success("Color updated successfully",
                adminCatalogService.updateColor(id, request));
    }

    @GetMapping("/sizes")
    @PreAuthorize("hasAuthority('COSTUME_VIEW')")
    public ApiResponse<List<AdminSizeResponse>> getSizes() {
        return ApiResponse.success("Sizes fetched successfully", adminCatalogService.getSizes());
    }

    @PostMapping("/sizes")
    @PreAuthorize("hasAuthority('COSTUME_CREATE')")
    public ApiResponse<AdminSizeResponse> createSize(@Valid @RequestBody AdminSizeRequest request) {
        return ApiResponse.success("Size created successfully", adminCatalogService.createSize(request));
    }

    @PutMapping("/sizes/{id}")
    @PreAuthorize("hasAuthority('COSTUME_UPDATE')")
    public ApiResponse<AdminSizeResponse> updateSize(
            @PathVariable UUID id, @Valid @RequestBody AdminSizeRequest request) {
        return ApiResponse.success("Size updated successfully",
                adminCatalogService.updateSize(id, request));
    }

    @GetMapping("/costumes/{costumeId}/variants")
    @PreAuthorize("hasAuthority('COSTUME_VIEW')")
    public ApiResponse<List<AdminVariantResponse>> getVariants(@PathVariable UUID costumeId) {
        return ApiResponse.success("Variants fetched successfully",
                adminCatalogService.getVariants(costumeId));
    }

    @PostMapping("/costumes/{costumeId}/variants")
    @PreAuthorize("hasAuthority('COSTUME_CREATE')")
    public ApiResponse<AdminVariantResponse> createVariant(
            @PathVariable UUID costumeId, @Valid @RequestBody AdminVariantRequest request) {
        return ApiResponse.success("Variant created successfully",
                adminCatalogService.createVariant(costumeId, request));
    }

    @PutMapping("/costumes/{costumeId}/variants/{variantId}")
    @PreAuthorize("hasAuthority('COSTUME_UPDATE')")
    public ApiResponse<AdminVariantResponse> updateVariant(
            @PathVariable UUID costumeId,
            @PathVariable UUID variantId,
            @Valid @RequestBody AdminVariantRequest request) {
        return ApiResponse.success("Variant updated successfully",
                adminCatalogService.updateVariant(costumeId, variantId, request));
    }
}
