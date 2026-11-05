package com.designer.costume.controller;

import com.designer.costume.dto.response.ApiResponse;
import com.designer.costume.dto.response.CategoryResponse;
import com.designer.costume.dto.response.PageResponse;
import com.designer.costume.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/categories")
@Tag(name = "Categories", description = "Read active costume categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    @Operation(summary = "List active categories")
    public ApiResponse<PageResponse<CategoryResponse>> getCategories(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
            @RequestParam(required = false) @Size(max = 100) String search) {
        return ApiResponse.success(
                "Categories fetched successfully",
                categoryService.getActiveCategories(page, size, search));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an active category by UUID")
    public ApiResponse<CategoryResponse> getCategory(@PathVariable UUID id) {
        return ApiResponse.success(
                "Category fetched successfully",
                categoryService.getActiveCategory(id));
    }
}
