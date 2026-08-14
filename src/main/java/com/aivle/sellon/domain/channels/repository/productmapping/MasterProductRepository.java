package com.aivle.sellon.domain.channels.repository.productmapping;

import com.aivle.sellon.domain.channels.entity.productmapping.MasterProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

import java.util.Optional;

public interface MasterProductRepository extends JpaRepository<MasterProduct, Long> {
    List<MasterProduct> findByCompany_Id(Long companyId);

    Optional<MasterProduct> findByCompany_IdAndProductGroupId(Long companyId, String productGroupId);

    @Query("SELECT mp FROM MasterProduct mp " +
            "WHERE mp.company.id = :companyId " +
            "AND (LOWER(mp.productName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(mp.productGroupId) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<MasterProduct> searchByKeyword(@Param("companyId") Long companyId, @Param("keyword") String keyword);

    long countByCompany_Id(Long companyId);
}
