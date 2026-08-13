package com.aivle.sellon.domain.channels.entity.productmapping;

import com.aivle.sellon.domain.company.entity.Company;
import com.aivle.sellon.global.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "master_product",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_master_product_company_master_sku",
                columnNames = {"company_id", "master_sku"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MasterProduct extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "master_product_key")
    private Long masterProductKey;

    @Column(name = "master_sku", nullable = false)
    private String masterSku;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    public static MasterProduct of(Company company, String masterSku, String productName) {
        MasterProduct entity = new MasterProduct();
        entity.company = company;
        entity.masterSku = masterSku;
        entity.productName = productName;
        return entity;
    }
}
