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
                name = "uk_master_product_company_product_group_id",
                columnNames = {"company_id", "product_group_id"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MasterProduct extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "master_product_key")
    private Long masterProductKey;

    @Column(name = "product_group_id", nullable = false)
    private String productGroupId;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    public static MasterProduct of(Company company, String productGroupId, String productName) {
        MasterProduct entity = new MasterProduct();
        entity.company = company;
        entity.productGroupId = productGroupId;
        entity.productName = productName;
        return entity;
    }
}
