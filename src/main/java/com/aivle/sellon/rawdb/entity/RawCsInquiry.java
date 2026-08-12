package com.aivle.sellon.rawdb.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 「Raw DB 스키마 확정 (8/7)」§2-4 cs. main server(우리) 소유·쓰기 대상.
 * 이상탐지·리포팅이 분모를 세는 정본이라, 분류 실패/미분류 건도 절대 지우지 않는다.
 * id는 CSV의 inquiry_id를 그대로 쓴다(§5-1 A안) - classification_worker가 이 값을
 * classified_item.item_id로 그대로 재사용하므로 여기서 값을 바꾸면 조인이 깨진다.
 */
@Entity
@Table(name = "cs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RawCsInquiry {

    @Id
    @Column(name = "id")
    private String id;

    @Column(name = "channel_product_id")
    private String channelProductId;

    @Column(name = "product_group_id")
    private String productGroupId;

    @Column(name = "channel_id")
    private String channelId;

    @Column(name = "content", nullable = false)
    private String content;

    @Column(name = "inquired_at", nullable = false)
    private LocalDateTime inquiredAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public static RawCsInquiry of(String id, String channelProductId, String productGroupId,
                                   String channelId, String content, LocalDateTime inquiredAt,
                                   LocalDateTime createdAt) {
        RawCsInquiry entity = new RawCsInquiry();
        entity.id = id;
        entity.channelProductId = channelProductId;
        entity.productGroupId = productGroupId;
        entity.channelId = channelId;
        entity.content = content;
        entity.inquiredAt = inquiredAt;
        entity.createdAt = createdAt;
        return entity;
    }
}
