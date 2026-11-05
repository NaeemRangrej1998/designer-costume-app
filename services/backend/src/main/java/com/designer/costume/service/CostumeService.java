package com.designer.costume.service;

import com.designer.costume.dto.response.CostumeResponse;
import com.designer.costume.dto.response.CostumeVariantResponse;
import com.designer.costume.dto.response.PageResponse;
import com.designer.costume.exception.ResourceNotFoundException;
import com.designer.costume.repository.costume.CostumeRepository;
import com.designer.costume.repository.variant.CostumeVariantRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CostumeService {

    private static final Sort COSTUME_SORT = Sort.by(Sort.Direction.ASC, "name");

    private final CostumeRepository costumeRepository;
    private final CostumeVariantRepository costumeVariantRepository;

    public CostumeService(
            CostumeRepository costumeRepository,
            CostumeVariantRepository costumeVariantRepository) {
        this.costumeRepository = costumeRepository;
        this.costumeVariantRepository = costumeVariantRepository;
    }

    public PageResponse<CostumeResponse> getActiveCostumes(
            int page, int size, String search, UUID categoryId) {
        Page<CostumeResponse> costumes = costumeRepository.findActiveCostumes(
                normalizeSearch(search),
                categoryId,
                PageRequest.of(page, size, COSTUME_SORT));
        return PageResponse.from(costumes);
    }

    public CostumeResponse getActiveCostume(UUID id) {
        return costumeRepository.findActiveCostumeById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Costume", id));
    }

    public List<CostumeVariantResponse> getAvailableVariants(UUID costumeId) {
        if (!costumeRepository.existsByIdAndActiveTrue(costumeId)) {
            throw new ResourceNotFoundException("Costume", costumeId);
        }
        return costumeVariantRepository.findAvailableVariantsByCostumeId(costumeId);
    }

    private String normalizeSearch(String search) {
        if (search == null || search.isBlank()) {
            return "";
        }
        return search.trim();
    }
}
