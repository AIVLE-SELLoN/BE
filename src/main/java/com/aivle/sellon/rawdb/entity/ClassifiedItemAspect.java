package com.aivle.sellon.rawdb.entity;

import com.aivle.sellon.domain.channels.enums.InquiryType;
import com.aivle.sellon.rawdb.entity.converter.InquiryTypeConverter;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 「Raw DB 스키마 확정 (8/7)」§2-6 classified_item_aspect. AI 노드(classification_worker)
 * 소유 - 우리는 읽기 전용이다. 1문의(item_id) : N aspect로 정규화되어 있다.
 * aspect 컬럼은 한글 라벨("색상" 등)이 그대로 저장되므로 InquiryTypeConverter로 변환한다.
 */
@Entity
@Table(name = "classified_item_aspect")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClassifiedItemAspect {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "item_id", nullable = false)
    private String itemId;

    @Convert(converter = InquiryTypeConverter.class)
    @Column(name = "aspect", nullable = false)
    private InquiryType aspect;

    @Column(name = "sentiment", nullable = false)
    private Integer sentiment;

    @Column(name = "mixed_signal")
    private Boolean mixedSignal;
}
