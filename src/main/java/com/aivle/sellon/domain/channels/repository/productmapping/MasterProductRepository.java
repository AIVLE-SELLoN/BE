package com.aivle.sellon.domain.channels.repository.productmapping;

import com.aivle.sellon.domain.channels.entity.productmapping.MasterProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

import java.util.Optional;

public interface MasterProductRepository extends JpaRepository<MasterProduct, Long> {
    List<MasterProduct> findByCompany_Id(Long companyId);

    /**
     * 매칭 툴 배치 결과의 mapped_product_code를 masterSku로 그대로 사용 — 이미 존재하면 재사용, 없으면 새로 생성.
     * master_sku 유니크 범위가 (company_id, master_sku)라 회사 조건 없이 조회하면 타 회사 상품에 잘못 연결될 수 있어
     * companyId까지 같이 조건으로 건다.
     */
    Optional<MasterProduct> findByCompany_IdAndMasterSku(Long companyId, String masterSku);

    @Query("SELECT mp FROM MasterProduct mp " +
            "WHERE mp.company.id = :companyId " +
            "AND (LOWER(mp.productName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(mp.masterSku) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<MasterProduct> searchByKeyword(@Param("companyId") Long companyId, @Param("keyword") String keyword);

    long countByCompany_Id(Long companyId);
}
