package com.designer.costume.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.designer.costume.dto.response.CategoryResponse;
import com.designer.costume.dto.response.CostumeResponse;
import com.designer.costume.dto.response.CostumeVariantResponse;
import com.designer.costume.dto.response.PageResponse;
import com.designer.costume.exception.ResourceNotFoundException;
import com.designer.costume.service.CategoryService;
import com.designer.costume.service.CostumeService;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import com.designer.costume.security.CustomUserDetailsService;
import com.designer.costume.security.JwtService;
import org.springframework.security.web.AuthenticationEntryPoint;

@WebMvcTest({CategoryController.class, CostumeController.class})
@WithMockUser(authorities = {"CATEGORY_VIEW", "COSTUME_VIEW"})
class CatalogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CategoryService categoryService;

    @MockitoBean
    private CostumeService costumeService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService userDetailsService;

    @MockitoBean
    private AuthenticationEntryPoint authenticationEntryPoint;

    @Test
    void categoryListReturnsPaginatedResponseAndPassesFilters() throws Exception {
        UUID categoryId = UUID.randomUUID();
        PageResponse<CategoryResponse> page = new PageResponse<>(
                List.of(new CategoryResponse(categoryId, "Wedding Wear", null, null, null, null)),
                0, 10, 1, 1, true);
        when(categoryService.getActiveCategories(0, 10, "wedding")).thenReturn(page);

        mockMvc.perform(get("/api/v1/categories")
                        .param("page", "0")
                        .param("size", "10")
                        .param("search", "wedding"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].name").value("Wedding Wear"))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.last").value(true));

        verify(categoryService).getActiveCategories(0, 10, "wedding");
    }

    @Test
    void categoryDetailReturnsDto() throws Exception {
        UUID categoryId = UUID.randomUUID();
        when(categoryService.getActiveCategory(categoryId))
                .thenReturn(new CategoryResponse(categoryId, "Bridal Lehenga", null, null, null, null));

        mockMvc.perform(get("/api/v1/categories/{id}", categoryId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(categoryId.toString()))
                .andExpect(jsonPath("$.data.name").value("Bridal Lehenga"));
    }

    @Test
    void missingCategoryReturnsConsistentNotFoundResponse() throws Exception {
        UUID categoryId = UUID.randomUUID();
        when(categoryService.getActiveCategory(categoryId))
                .thenThrow(new ResourceNotFoundException("Category", categoryId));

        mockMvc.perform(get("/api/v1/categories/{id}", categoryId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Category not found: " + categoryId))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void categoryListRejectsInvalidPagination() throws Exception {
        mockMvc.perform(get("/api/v1/categories").param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void categoryListRejectsNegativePage() throws Exception {
        mockMvc.perform(get("/api/v1/categories").param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void costumeListReturnsPagedDtosAndUsesOptionalFilters() throws Exception {
        UUID categoryId = UUID.randomUUID();
        UUID costumeId = UUID.randomUUID();
        PageResponse<CostumeResponse> page = new PageResponse<>(
                List.of(new CostumeResponse(
                        costumeId, "Bridal Lehenga", null, "BR-001", new BigDecimal("1250.00"),
                        categoryId, "Wedding Wear", "https://images.example.test/bridal.jpg")),
                1, 5, 6, 2, true);
        when(costumeService.getActiveCostumes(1, 5, "bridal", categoryId)).thenReturn(page);

        mockMvc.perform(get("/api/v1/costumes")
                        .param("page", "1")
                        .param("size", "5")
                        .param("search", "bridal")
                        .param("categoryId", categoryId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].name").value("Bridal Lehenga"))
                .andExpect(jsonPath("$.data.content[0].category.name").doesNotExist())
                .andExpect(jsonPath("$.data.content[0].categoryName").value("Wedding Wear"))
                .andExpect(jsonPath("$.data.content[0].primaryImageUrl")
                        .value("https://images.example.test/bridal.jpg"))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(5));

        verify(costumeService).getActiveCostumes(1, 5, "bridal", categoryId);
    }

    @Test
    void costumeDetailReturnsDto() throws Exception {
        UUID costumeId = UUID.randomUUID();
        when(costumeService.getActiveCostume(costumeId))
                .thenReturn(new CostumeResponse(
                        costumeId, "Classic Sherwani", "Description", "SH-001",
                        new BigDecimal("900.00"), UUID.randomUUID(), "Traditional Wear", null));

        mockMvc.perform(get("/api/v1/costumes/{id}", costumeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(costumeId.toString()))
                .andExpect(jsonPath("$.data.name").value("Classic Sherwani"))
                .andExpect(jsonPath("$.data.categoryName").value("Traditional Wear"));
    }

    @Test
    void missingCostumeReturnsNotFound() throws Exception {
        UUID costumeId = UUID.randomUUID();
        when(costumeService.getActiveCostume(costumeId))
                .thenThrow(new ResourceNotFoundException("Costume", costumeId));

        mockMvc.perform(get("/api/v1/costumes/{id}", costumeId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Costume not found: " + costumeId));
    }

    @Test
    void variantsReturnsColorSizeAndPriceFields() throws Exception {
        UUID costumeId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();
        when(costumeService.getAvailableVariants(costumeId))
                .thenReturn(List.of(new CostumeVariantResponse(
                        variantId, costumeId, "Ruby Red", "#AA1122", "M", 2,
                        new BigDecimal("975.00"), true)));

        mockMvc.perform(get("/api/v1/costumes/{costumeId}/variants", costumeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].colorName").value("Ruby Red"))
                .andExpect(jsonPath("$.data[0].colorHexCode").value("#AA1122"))
                .andExpect(jsonPath("$.data[0].sizeName").value("M"))
                .andExpect(jsonPath("$.data[0].price").value(975.00))
                .andExpect(jsonPath("$.data[0].available").value(true));
    }

    @Test
    void missingCostumeVariantsReturnsNotFound() throws Exception {
        UUID costumeId = UUID.randomUUID();
        when(costumeService.getAvailableVariants(costumeId))
                .thenThrow(new ResourceNotFoundException("Costume", costumeId));

        mockMvc.perform(get("/api/v1/costumes/{costumeId}/variants", costumeId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Costume not found: " + costumeId));
    }

    @Test
    void invalidCategoryIdReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/costumes").param("categoryId", "not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid value for parameter: categoryId"));
    }
}
