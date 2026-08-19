package com.aivle.sellon.domain.channels.entity.productmapping;

import com.aivle.sellon.global.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * "건너뜀" 처리 여부를 메인 db에 별도로 보관한다.
 * raw db(mapped_data)는 hbm2ddl.auto=validate로 고정돼 있어 백엔드가 스키마를 건드릴 수 없기 때문에
 * (RawDataSourceConfig 참고), raw db 컬럼을 늘리는 대신 메인 db에 얇은 매핑 테이블을 둔다.
 */
@Entity
@Table(
        name = "skipped_mapping",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_skipped_mapping_variant_row_id",
                columnNames = {"variant_row_id"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SkippedMapping extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "variant_row_id", nullable = false)
    private String variantRowId;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    private SkippedMapping(String variantRowId, Long companyId) {
        this.variantRowId = variantRowId;
        this.companyId = companyId;
    }

    public static SkippedMapping of(String variantRowId, Long companyId) {
        return new SkippedMapping(variantRowId, companyId);
    }
}
