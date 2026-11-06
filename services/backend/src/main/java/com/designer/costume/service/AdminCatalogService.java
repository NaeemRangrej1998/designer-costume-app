package com.designer.costume.service;

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
import com.designer.costume.dto.response.PageResponse;
import com.designer.costume.entity.category.CategoryEntity;
import com.designer.costume.entity.costume.CostumeEntity;
import com.designer.costume.entity.costume.CostumeImageEntity;
import com.designer.costume.entity.variant.ColorEntity;
import com.designer.costume.entity.variant.CostumeVariantEntity;
import com.designer.costume.entity.variant.SizeEntity;
import com.designer.costume.enums.CostumeImageType;
import com.designer.costume.exception.BadRequestException;
import com.designer.costume.exception.ResourceConflictException;
import com.designer.costume.exception.ResourceNotFoundException;
import com.designer.costume.repository.category.CategoryRepository;
import com.designer.costume.repository.costume.CostumeImageRepository;
import com.designer.costume.repository.costume.CostumeRepository;
import com.designer.costume.repository.variant.ColorRepository;
import com.designer.costume.repository.variant.CostumeVariantRepository;
import com.designer.costume.repository.variant.SizeRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AdminCatalogService {

    private static final Sort CATEGORY_SORT = Sort.by(Sort.Direction.ASC, "name");
    private static final Sort COSTUME_SORT = Sort.by(Sort.Direction.ASC, "name");

    private final CategoryRepository categoryRepository;
    private final CostumeRepository costumeRepository;
    private final CostumeImageRepository costumeImageRepository;
    private final ColorRepository colorRepository;
    private final SizeRepository sizeRepository;
    private final CostumeVariantRepository variantRepository;
    private final AdminActivityService activityService;

    public AdminCatalogService(
            CategoryRepository categoryRepository,
            CostumeRepository costumeRepository,
            CostumeImageRepository costumeImageRepository,
            ColorRepository colorRepository,
            SizeRepository sizeRepository,
            CostumeVariantRepository variantRepository,
            AdminActivityService activityService) {
        this.categoryRepository = categoryRepository;
        this.costumeRepository = costumeRepository;
        this.costumeImageRepository = costumeImageRepository;
        this.colorRepository = colorRepository;
        this.sizeRepository = sizeRepository;
        this.variantRepository = variantRepository;
        this.activityService = activityService;
    }

    public PageResponse<AdminCategoryResponse> getCategories(
            int page, int size, String search, Boolean active) {
        Page<CategoryEntity> result = categoryRepository.findAdminCategories(
                normalizeSearch(search), active, PageRequest.of(page, size, CATEGORY_SORT));
        return PageResponse.from(result.map(this::toCategoryResponse));
    }

    public AdminCategoryResponse getCategory(UUID id) {
        return toCategoryResponse(findCategory(id));
    }

    @Transactional
    public AdminCategoryResponse createCategory(AdminCategoryRequest request) {
        String name = request.name().trim();
        ensureCategoryNameAvailable(name, null);
        CategoryEntity category = new CategoryEntity();
        applyCategory(category, request, name);
        CategoryEntity saved = categoryRepository.save(category);
        activityService.record("CREATED", "CATEGORY", saved.getId());
        return toCategoryResponse(saved);
    }

    @Transactional
    public AdminCategoryResponse updateCategory(UUID id, AdminCategoryRequest request) {
        CategoryEntity category = findCategory(id);
        String name = request.name().trim();
        ensureCategoryNameAvailable(name, id);
        applyCategory(category, request, name);
        CategoryEntity saved = categoryRepository.save(category);
        activityService.record("UPDATED", "CATEGORY", saved.getId());
        return toCategoryResponse(saved);
    }

    @Transactional
    public void deactivateCategory(UUID id) {
        CategoryEntity category = findCategory(id);
        if (costumeRepository.existsByCategoryIdAndActiveTrue(id)) {
            throw new ResourceConflictException("Category has active costumes and cannot be deactivated");
        }
        category.setActive(false);
        activityService.record("DEACTIVATED", "CATEGORY", id);
    }

    public PageResponse<AdminCostumeResponse> getCostumes(
            int page, int size, String search, UUID categoryId, Boolean active) {
        Page<CostumeEntity> result = costumeRepository.findAdminCostumes(
                normalizeSearch(search),
                categoryId,
                active,
                PageRequest.of(page, size, COSTUME_SORT));
        return PageResponse.from(result.map(this::toCostumeResponse));
    }

    public AdminCostumeResponse getCostume(UUID id) {
        return toCostumeResponse(findCostume(id));
    }

    @Transactional
    public AdminCostumeResponse createCostume(AdminCostumeRequest request) {
        String sku = request.sku().trim();
        ensureCostumeSkuAvailable(sku, null);
        CostumeEntity costume = new CostumeEntity();
        applyCostume(costume, request, sku);
        CostumeEntity saved = costumeRepository.save(costume);
        updatePrimaryImage(saved, request.primaryImageUrl());
        activityService.record("CREATED", "COSTUME", saved.getId());
        return toCostumeResponse(saved);
    }

    @Transactional
    public AdminCostumeResponse updateCostume(UUID id, AdminCostumeRequest request) {
        CostumeEntity costume = findCostume(id);
        String sku = request.sku().trim();
        ensureCostumeSkuAvailable(sku, id);
        applyCostume(costume, request, sku);
        updatePrimaryImage(costume, request.primaryImageUrl());
        CostumeEntity saved = costumeRepository.save(costume);
        activityService.record("UPDATED", "COSTUME", saved.getId());
        return toCostumeResponse(saved);
    }

    @Transactional
    public void deactivateCostume(UUID id) {
        findCostume(id).setActive(false);
        activityService.record("DEACTIVATED", "COSTUME", id);
    }

    public List<AdminColorResponse> getColors() {
        return colorRepository.findAllByOrderByNameAsc().stream()
                .map(this::toColorResponse)
                .toList();
    }

    @Transactional
    public AdminColorResponse createColor(AdminColorRequest request) {
        String name = request.name().trim();
        ensureColorNameAvailable(name, null);
        ColorEntity color = new ColorEntity();
        applyColor(color, request, name);
        ColorEntity saved = colorRepository.save(color);
        activityService.record("CREATED", "COLOR", saved.getId());
        return toColorResponse(saved);
    }

    @Transactional
    public AdminColorResponse updateColor(UUID id, AdminColorRequest request) {
        ColorEntity color = colorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Color", id));
        String name = request.name().trim();
        ensureColorNameAvailable(name, id);
        applyColor(color, request, name);
        ColorEntity saved = colorRepository.save(color);
        activityService.record("UPDATED", "COLOR", saved.getId());
        return toColorResponse(saved);
    }

    public List<AdminSizeResponse> getSizes() {
        return sizeRepository.findAllByOrderByDisplayOrderAscNameAsc().stream()
                .map(this::toSizeResponse)
                .toList();
    }

    @Transactional
    public AdminSizeResponse createSize(AdminSizeRequest request) {
        String name = request.name().trim();
        ensureSizeNameAvailable(name, null);
        SizeEntity size = new SizeEntity();
        applySize(size, request, name);
        SizeEntity saved = sizeRepository.save(size);
        activityService.record("CREATED", "SIZE", saved.getId());
        return toSizeResponse(saved);
    }

    @Transactional
    public AdminSizeResponse updateSize(UUID id, AdminSizeRequest request) {
        SizeEntity size = sizeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Size", id));
        String name = request.name().trim();
        ensureSizeNameAvailable(name, id);
        applySize(size, request, name);
        SizeEntity saved = sizeRepository.save(size);
        activityService.record("UPDATED", "SIZE", saved.getId());
        return toSizeResponse(saved);
    }

    public List<AdminVariantResponse> getVariants(UUID costumeId) {
        findCostume(costumeId);
        return variantRepository.findAllByCostumeId(costumeId).stream()
                .map(this::toVariantResponse)
                .toList();
    }

    @Transactional
    public AdminVariantResponse createVariant(UUID costumeId, AdminVariantRequest request) {
        CostumeEntity costume = findCostume(costumeId);
        ColorEntity color = findColor(request.colorId());
        SizeEntity size = findSize(request.sizeId());
        validateAvailableVariant(request.available(), color, size);
        String sku = request.sku().trim();
        ensureVariantSkuAvailable(sku, null);
        if (variantRepository.findByCostumeIdAndColorIdAndSizeId(
                costumeId, color.getId(), size.getId()).isPresent()) {
            throw new ResourceConflictException("A variant already exists for this color and size");
        }
        CostumeVariantEntity variant = new CostumeVariantEntity();
        variant.setCostume(costume);
        applyVariant(variant, request, color, size, sku);
        CostumeVariantEntity saved = variantRepository.save(variant);
        activityService.record("CREATED", "COSTUME_VARIANT", saved.getId());
        return toVariantResponse(saved);
    }

    @Transactional
    public AdminVariantResponse updateVariant(
            UUID costumeId, UUID variantId, AdminVariantRequest request) {
        CostumeVariantEntity variant = variantRepository.findById(variantId)
                .filter(item -> item.getCostume().getId().equals(costumeId))
                .orElseThrow(() -> new ResourceNotFoundException("Costume variant", variantId));
        ColorEntity color = findColor(request.colorId());
        SizeEntity size = findSize(request.sizeId());
        validateAvailableVariant(request.available(), color, size);
        String sku = request.sku().trim();
        ensureVariantSkuAvailable(sku, variantId);
        variantRepository.findByCostumeIdAndColorIdAndSizeId(
                        costumeId, color.getId(), size.getId())
                .filter(existing -> !existing.getId().equals(variantId))
                .ifPresent(existing -> {
                    throw new ResourceConflictException(
                            "A variant already exists for this color and size");
                });
        applyVariant(variant, request, color, size, sku);
        CostumeVariantEntity saved = variantRepository.save(variant);
        activityService.record("UPDATED", "COSTUME_VARIANT", saved.getId());
        return toVariantResponse(saved);
    }

    private void applyCategory(CategoryEntity category, AdminCategoryRequest request, String name) {
        if (!request.active()
                && category.getId() != null
                && costumeRepository.existsByCategoryIdAndActiveTrue(category.getId())) {
            throw new ResourceConflictException("Category has active costumes and cannot be deactivated");
        }
        category.setName(name);
        category.setDescription(normalizeNullable(request.description()));
        category.setImageUrl(normalizeNullable(request.imageUrl()));
        category.setActive(request.active());
        if (request.parentCategoryId() == null) {
            category.setParentCategory(null);
            return;
        }
        UUID parentId = request.parentCategoryId();
        if (parentId.equals(category.getId())) {
            throw new BadRequestException("A category cannot be its own parent");
        }
        CategoryEntity parent = findCategory(parentId);
        if (!parent.isActive()) {
            throw new BadRequestException("Parent category must be active");
        }
        ensureNoCategoryCycle(category.getId(), parent);
        category.setParentCategory(parent);
    }

    private void ensureNoCategoryCycle(UUID categoryId, CategoryEntity parent) {
        if (categoryId == null) {
            return;
        }
        HashSet<UUID> visited = new HashSet<>();
        CategoryEntity current = parent;
        while (current != null) {
            if (!visited.add(current.getId())) {
                throw new BadRequestException("Category hierarchy contains a cycle");
            }
            if (categoryId.equals(current.getId())) {
                throw new BadRequestException("Category cannot be nested under its descendant");
            }
            current = current.getParentCategory();
        }
    }

    private void applyCostume(CostumeEntity costume, AdminCostumeRequest request, String sku) {
        CategoryEntity category = findCategory(request.categoryId());
        if (request.active() && !category.isActive()) {
            throw new BadRequestException("An active costume must belong to an active category");
        }
        costume.setName(request.name().trim());
        costume.setDescription(normalizeNullable(request.description()));
        costume.setSku(sku);
        costume.setCategory(category);
        costume.setBasePrice(request.basePrice());
        costume.setActive(request.active());
    }

    private void updatePrimaryImage(CostumeEntity costume, String imageUrl) {
        List<CostumeImageEntity> images =
                costumeImageRepository.findAllByCostumeIdOrderByDisplayOrderAsc(costume.getId());
        String normalizedUrl = normalizeNullable(imageUrl);
        CostumeImageEntity primary = images.stream()
                .filter(CostumeImageEntity::isPrimary)
                .findFirst()
                .orElse(null);
        if (normalizedUrl != null && normalizedUrl.startsWith("/api/v1/costumes/")) {
            CostumeImageEntity selected = images.stream()
                    .filter(image -> normalizedUrl.equals(image.getImageUrl()))
                    .findFirst()
                    .orElseThrow(() -> new BadRequestException(
                            "Select an existing uploaded image from the costume gallery"));
            images.forEach(image -> image.setPrimary(false));
            costumeImageRepository.flush();
            selected.setPrimary(true);
            return;
        }
        if (primary != null && primary.getStorageKey() != null) {
            throw new BadRequestException("Manage uploaded primary images through the costume gallery");
        }
        images.forEach(image -> image.setPrimary(false));
        if (normalizedUrl == null) {
            return;
        }
        if (primary == null) {
            primary = new CostumeImageEntity();
            primary.setCostume(costume);
            primary.setImageType(CostumeImageType.PRODUCT);
            primary.setDisplayOrder(0);
        }
        primary.setImageUrl(normalizedUrl);
        primary.setPrimary(true);
        costumeImageRepository.save(primary);
    }

    private void applyColor(ColorEntity color, AdminColorRequest request, String name) {
        color.setName(name);
        color.setHexCode(request.hexCode().toUpperCase(Locale.ROOT));
        color.setActive(request.active());
    }

    private void applySize(SizeEntity size, AdminSizeRequest request, String name) {
        size.setName(name);
        size.setDisplayOrder(request.displayOrder());
        size.setActive(request.active());
    }

    private void applyVariant(
            CostumeVariantEntity variant,
            AdminVariantRequest request,
            ColorEntity color,
            SizeEntity size,
            String sku) {
        variant.setColor(color);
        variant.setSize(size);
        variant.setSku(sku);
        variant.setPrice(request.price());
        variant.setStockQuantity(request.stockQuantity());
        variant.setImageUrl(normalizeNullable(request.imageUrl()));
        variant.setAvailable(request.available());
    }

    private void validateAvailableVariant(boolean available, ColorEntity color, SizeEntity size) {
        if (available && (!color.isActive() || !size.isActive())) {
            throw new BadRequestException("Available variants require active colors and sizes");
        }
    }

    private void ensureCategoryNameAvailable(String name, UUID currentId) {
        categoryRepository.findByNameIgnoreCase(name)
                .filter(existing -> !existing.getId().equals(currentId))
                .ifPresent(existing -> {
                    throw new ResourceConflictException("Category name is already in use");
                });
    }

    private void ensureCostumeSkuAvailable(String sku, UUID currentId) {
        costumeRepository.findBySku(sku)
                .filter(existing -> !existing.getId().equals(currentId))
                .ifPresent(existing -> {
                    throw new ResourceConflictException("Costume SKU is already in use");
                });
    }

    private void ensureColorNameAvailable(String name, UUID currentId) {
        colorRepository.findByNameIgnoreCase(name)
                .filter(existing -> !existing.getId().equals(currentId))
                .ifPresent(existing -> {
                    throw new ResourceConflictException("Color name is already in use");
                });
    }

    private void ensureSizeNameAvailable(String name, UUID currentId) {
        sizeRepository.findByNameIgnoreCase(name)
                .filter(existing -> !existing.getId().equals(currentId))
                .ifPresent(existing -> {
                    throw new ResourceConflictException("Size name is already in use");
                });
    }

    private void ensureVariantSkuAvailable(String sku, UUID currentId) {
        variantRepository.findBySku(sku)
                .filter(existing -> !existing.getId().equals(currentId))
                .ifPresent(existing -> {
                    throw new ResourceConflictException("Variant SKU is already in use");
                });
    }

    private CategoryEntity findCategory(UUID id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));
    }

    private CostumeEntity findCostume(UUID id) {
        return costumeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Costume", id));
    }

    private ColorEntity findColor(UUID id) {
        return colorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Color", id));
    }

    private SizeEntity findSize(UUID id) {
        return sizeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Size", id));
    }

    private AdminCategoryResponse toCategoryResponse(CategoryEntity category) {
        CategoryEntity parent = category.getParentCategory();
        return new AdminCategoryResponse(
                category.getId(),
                category.getName(),
                category.getDescription(),
                category.getImageUrl(),
                parent == null ? null : parent.getId(),
                parent == null ? null : parent.getName(),
                category.isActive());
    }

    private AdminCostumeResponse toCostumeResponse(CostumeEntity costume) {
        String primaryImageUrl = costumeImageRepository
                .findAllByCostumeIdOrderByDisplayOrderAsc(costume.getId()).stream()
                .filter(CostumeImageEntity::isPrimary)
                .map(CostumeImageEntity::getImageUrl)
                .findFirst()
                .orElse(null);
        return new AdminCostumeResponse(
                costume.getId(),
                costume.getName(),
                costume.getDescription(),
                costume.getSku(),
                costume.getBasePrice(),
                costume.getCategory().getId(),
                costume.getCategory().getName(),
                primaryImageUrl,
                costume.isActive());
    }

    private AdminColorResponse toColorResponse(ColorEntity color) {
        return new AdminColorResponse(color.getId(), color.getName(), color.getHexCode(), color.isActive());
    }

    private AdminSizeResponse toSizeResponse(SizeEntity size) {
        return new AdminSizeResponse(size.getId(), size.getName(), size.getDisplayOrder(), size.isActive());
    }

    private AdminVariantResponse toVariantResponse(CostumeVariantEntity variant) {
        return new AdminVariantResponse(
                variant.getId(),
                variant.getCostume().getId(),
                variant.getColor().getId(),
                variant.getColor().getName(),
                variant.getColor().getHexCode(),
                variant.getSize().getId(),
                variant.getSize().getName(),
                variant.getSize().getDisplayOrder(),
                variant.getSku(),
                variant.getPrice(),
                variant.getStockQuantity(),
                variant.getImageUrl(),
                variant.isAvailable());
    }

    private String normalizeSearch(String search) {
        return search == null || search.isBlank() ? "" : search.trim();
    }

    private String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
