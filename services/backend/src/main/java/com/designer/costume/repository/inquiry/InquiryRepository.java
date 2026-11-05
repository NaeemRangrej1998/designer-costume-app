package com.designer.costume.repository.inquiry;

import com.designer.costume.entity.inquiry.InquiryEntity;
import com.designer.costume.enums.InquiryStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InquiryRepository extends JpaRepository<InquiryEntity, UUID> {

    long countByStatus(InquiryStatus status);

    List<InquiryEntity> findAllByStatusOrderByCreatedAtDesc(InquiryStatus status);

    @Query(
            value = """
                    SELECT inquiry FROM InquiryEntity inquiry
                    WHERE (:search = '' OR LOWER(inquiry.customerName) LIKE LOWER(CONCAT('%', :search, '%'))
                           OR LOWER(inquiry.email) LIKE LOWER(CONCAT('%', :search, '%')))
                      AND (:status IS NULL OR inquiry.status = :status)
                    """,
            countQuery = """
                    SELECT COUNT(inquiry) FROM InquiryEntity inquiry
                    WHERE (:search = '' OR LOWER(inquiry.customerName) LIKE LOWER(CONCAT('%', :search, '%'))
                           OR LOWER(inquiry.email) LIKE LOWER(CONCAT('%', :search, '%')))
                      AND (:status IS NULL OR inquiry.status = :status)
                    """)
    Page<InquiryEntity> findAdminInquiries(
            @Param("search") String search,
            @Param("status") InquiryStatus status,
            Pageable pageable);
}
