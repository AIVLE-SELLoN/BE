package com.aivle.sellon.rawdb.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 「Raw DB 스키마 확정 (8/7)」§2-6 classified_item. AI 노드(classification_worker) 소유 -
 * 우리는 읽기 전용이다. item_id는 cs.id/reviews.id를 그대로 재사용한다(§5-1 A안).
 */
@Entity
@Table(name = "classified_item")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClassifiedItem {

    @Id
    @Column(name = "item_id")
    private String itemId;

    @Column(name = "source")
    private String source;

    @Column(name = "classified_at")
    private LocalDateTime classifiedAt;

    @Column(name = "prompt_version")
    private String promptVersion;
}
