package com.designer.costume.repository.inquiry;

import com.designer.costume.entity.inquiry.InquiryEntity;
import com.designer.costume.enums.InquiryStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InquiryRepository extends JpaRepository<InquiryEntity, UUID> {

    long countByStatus(InquiryStatus status);

    List<InquiryEntity> findAllByStatusOrderByCreatedAtDesc(InquiryStatus status);
}
