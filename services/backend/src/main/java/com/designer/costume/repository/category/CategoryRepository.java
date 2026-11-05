package com.designer.costume.repository.category;

import com.designer.costume.entity.category.CategoryEntity;
import com.designer.costume.dto.response.CategoryResponse;
import org.springframework.data.domain.Sort;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CategoryRepository extends JpaRepository<CategoryEntity, UUID> {

    long countByActiveTrue();

    Optional<CategoryEntity> findByNameIgnoreCase(String name);

    @Query(
            value = """
                    SELECT c FROM CategoryEntity c
                    WHERE (:search = '' OR LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%')))
                      AND (:active IS NULL OR c.active = :active)
                    """,
            countQuery = """
                    SELECT COUNT(c) FROM CategoryEntity c
                    WHERE (:search = '' OR LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%')))
                      AND (:active IS NULL OR c.active = :active)
                    """)
    Page<CategoryEntity> findAdminCategories(
            @Param("search") String search,
            @Param("active") Boolean active,
            Pageable pageable);

    @Query(
            value = """
                    SELECT new com.designer.costume.dto.response.CategoryResponse(
                        c.id, c.name, c.description, c.imageUrl,
                        parent.id, parent.name
                    )
                    FROM CategoryEntity c
                    LEFT JOIN c.parentCategory parent
                    WHERE c.active = true
                      AND (:search = '' OR LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%')))
                    """,
            countQuery = """
                    SELECT COUNT(c)
                    FROM CategoryEntity c
                    WHERE c.active = true
                      AND (:search = '' OR LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%')))
                    """)
    Page<CategoryResponse> findActiveCategories(
            @Param("search") String search, Pageable pageable);

    @Query("""
            SELECT new com.designer.costume.dto.response.CategoryResponse(
                c.id, c.name, c.description, c.imageUrl,
                parent.id, parent.name
            )
            FROM CategoryEntity c
            LEFT JOIN c.parentCategory parent
            WHERE c.id = :id AND c.active = true
            """)
    Optional<CategoryResponse> findActiveCategoryById(@Param("id") UUID id);
}
