package com.designer.costume.service;

import com.designer.costume.dto.response.CategoryResponse;
import com.designer.costume.dto.response.PageResponse;
import com.designer.costume.exception.ResourceNotFoundException;
import com.designer.costume.repository.category.CategoryRepository;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CategoryService {

    private static final Sort CATEGORY_SORT = Sort.by(Sort.Direction.ASC, "name");

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public PageResponse<CategoryResponse> getActiveCategories(
            int page, int size, String search) {
        Page<CategoryResponse> categories = categoryRepository.findActiveCategories(
                normalizeSearch(search),
                PageRequest.of(page, size, CATEGORY_SORT));
        return PageResponse.from(categories);
    }

    public CategoryResponse getActiveCategory(UUID id) {
        return categoryRepository.findActiveCategoryById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));
    }

    private String normalizeSearch(String search) {
        if (search == null || search.isBlank()) {
            return "";
        }
        return search.trim();
    }
}
