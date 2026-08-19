package com.aivle.sellon.domain.channels.repository.productmapping;

import com.aivle.sellon.domain.channels.entity.productmapping.SkippedMapping;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Set;

public interface SkippedMappingRepository extends JpaRepository<SkippedMapping, Long> {
    boolean existsByVariantRowId(String variantRowId);

    List<SkippedMapping> findByCompanyIdAndVariantRowIdIn(Long companyId, Set<String> variantRowIds);

    void deleteByVariantRowId(String variantRowId);
}
