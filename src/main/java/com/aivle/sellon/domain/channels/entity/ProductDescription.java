package com.aivle.sellon.domain.channels.entity;

import com.aivle.sellon.domain.user.entity.User;
import com.aivle.sellon.global.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 채널별 상품 상세 설명의 "현재 값" — 개선안 승인 시 실제로 반영되고, 롤백 시 되돌아가는 대상.
// 외부 채널(쿠팡/네이버 등) API 연동은 아직 없어 우리 DB에만 저장한다(채널 도메인 최소 구현).
@Entity
@Table(name = "product_description")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductDescription extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_description_key")
    private Long productDescriptionKey;

    @Column(name = "product_group_id", length = 100, nullable = false)
    private String productGroupId;

    @Column(name = "channel", length = 10, nullable = false)
    private String channel;

    @Column(name = "description", length = 2000, nullable = false)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "root_user_id", nullable = false)
    private User rootUser;

    public static ProductDescription of(User rootUser, String productGroupId, String channel, String description) {
        ProductDescription entity = new ProductDescription();
        entity.rootUser = rootUser;
        entity.productGroupId = productGroupId;
        entity.channel = channel;
        entity.description = description;
        return entity;
    }

    public void update(String description) {
        this.description = description;
    }
}
