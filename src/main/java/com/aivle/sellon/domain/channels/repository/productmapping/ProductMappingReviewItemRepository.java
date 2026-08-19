package com.aivle.sellon.domain.channels.repository.productmapping;

import com.aivle.sellon.domain.channels.entity.productmapping.ProductMappingReviewItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductMappingReviewItemRepository extends JpaRepository<ProductMappingReviewItem, Long> {
    List<ProductMappingReviewItem> findByCompany_IdAndResolved(Long companyId, boolean resolved);
}
