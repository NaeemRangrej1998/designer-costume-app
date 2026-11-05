package com.designer.costume.repository.costume;

import com.designer.costume.entity.costume.CostumeEntity;
import com.designer.costume.dto.response.CostumeResponse;
import com.designer.costume.dto.response.DashboardCategoryCountResponse;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CostumeRepository extends JpaRepository<CostumeEntity, UUID> {

    long countByActiveTrue();

    @Query("""
            SELECT new com.designer.costume.dto.response.DashboardCategoryCountResponse(
                category.name, COUNT(costume.id)
            )
            FROM CategoryEntity category
            LEFT JOIN CostumeEntity costume
                ON costume.category.id = category.id AND costume.active = true
            WHERE category.active = true
            GROUP BY category.id, category.name
            ORDER BY category.name
            """)
    List<DashboardCategoryCountResponse> countActiveCostumesByActiveCategory();

    Optional<CostumeEntity> findBySku(String sku);

    @Query(
            value = """
                    SELECT c FROM CostumeEntity c
                    WHERE (:search = '' OR LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%'))
                           OR LOWER(c.sku) LIKE LOWER(CONCAT('%', :search, '%')))
                      AND (:categoryId IS NULL OR c.category.id = :categoryId)
                      AND (:active IS NULL OR c.active = :active)
                    """,
            countQuery = """
                    SELECT COUNT(c) FROM CostumeEntity c
                    WHERE (:search = '' OR LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%'))
                           OR LOWER(c.sku) LIKE LOWER(CONCAT('%', :search, '%')))
                      AND (:categoryId IS NULL OR c.category.id = :categoryId)
                      AND (:active IS NULL OR c.active = :active)
                    """)
    Page<CostumeEntity> findAdminCostumes(
            @Param("search") String search,
            @Param("categoryId") UUID categoryId,
            @Param("active") Boolean active,
            Pageable pageable);

    List<CostumeEntity> findByNameContainingIgnoreCase(String name);

    boolean existsByIdAndActiveTrue(UUID id);

    boolean existsByCategoryIdAndActiveTrue(UUID categoryId);

    @Query(
            value = """
                    SELECT new com.designer.costume.dto.response.CostumeResponse(
                        c.id, c.name, c.description, c.sku, c.basePrice,
                        category.id, category.name, image.imageUrl
                    )
                    FROM CostumeEntity c
                    JOIN c.category category
                    LEFT JOIN c.images image ON image.primary = true
                    WHERE c.active = true
                      AND (:search = '' OR LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%')))
                      AND (:categoryId IS NULL OR category.id = :categoryId)
                    """,
            countQuery = """
                    SELECT COUNT(c)
                    FROM CostumeEntity c
                    WHERE c.active = true
                      AND (:search = '' OR LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%')))
                      AND (:categoryId IS NULL OR c.category.id = :categoryId)
                    """)
    Page<CostumeResponse> findActiveCostumes(
            @Param("search") String search,
            @Param("categoryId") UUID categoryId,
            Pageable pageable);

    @Query("""
            SELECT new com.designer.costume.dto.response.CostumeResponse(
                c.id, c.name, c.description, c.sku, c.basePrice,
                category.id, category.name, image.imageUrl
            )
            FROM CostumeEntity c
            JOIN c.category category
            LEFT JOIN c.images image ON image.primary = true
            WHERE c.id = :id AND c.active = true
            """)
    Optional<CostumeResponse> findActiveCostumeById(@Param("id") UUID id);
}
